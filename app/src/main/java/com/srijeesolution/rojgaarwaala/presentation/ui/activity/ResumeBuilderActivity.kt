package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.databinding.ActivityResumeBuilderBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.resume.Education
import com.srijeesolution.rojgaarwaala.utils.resume.Experience
import com.srijeesolution.rojgaarwaala.utils.resume.ResumeCopy
import com.srijeesolution.rojgaarwaala.utils.resume.ResumeData
import com.srijeesolution.rojgaarwaala.utils.resume.ResumeLanguage
import com.srijeesolution.rojgaarwaala.utils.resume.ResumePdfWriter
import com.srijeesolution.rojgaarwaala.utils.resume.ResumeRules
import com.srijeesolution.rojgaarwaala.utils.resume.ResumeStore
import com.srijeesolution.rojgaarwaala.utils.ChhattisgarhDistricts
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

/**
 * Four short steps that end in a PDF resume made on the phone. The finished file goes
 * back to whoever opened the builder (Profile or the apply form) to be uploaded the
 * same way a picked resume is.
 */
@AndroidEntryPoint
class ResumeBuilderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PDF_PATH = "resume_pdf_path"
        private const val STEPS = 4
        private val PLACEHOLDER_NAME = Regex("""(?i)user_?\d+""")

        fun intent(context: Context) = Intent(context, ResumeBuilderActivity::class.java)
    }

    private lateinit var binding: ActivityResumeBuilderBinding
    private var data = ResumeData()
    private var step = 1
    private var previewJob: Job? = null

    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { savePhoto(it) }
    }

    private var selfieUri: Uri? = null

    /** The camera app, asked to open on the front lens where it supports that. */
    private val selfieTaker = registerForActivityResult(object : ActivityResultContracts.TakePicture() {
        override fun createIntent(context: Context, input: Uri): Intent = super.createIntent(context, input).apply {
            putExtra("android.intent.extras.CAMERA_FACING", 1)
            putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
            putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
        }
    }) { taken ->
        val uri = selfieUri
        if (taken && uri != null) savePhoto(uri)
    }

    private var pendingDownload: File? = null

    private val downloader = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { target ->
        val source = pendingDownload
        pendingDownload = null
        if (target == null || source == null) return@registerForActivityResult
        val saved = runCatching {
            contentResolver.openOutputStream(target)?.use { out -> source.inputStream().use { it.copyTo(out) } } != null
        }.getOrDefault(false)
        Toast.makeText(
            this,
            if (saved) "रिज्यूमे डाउनलोड हो गया" else "डाउनलोड नहीं हो पाया, दोबारा कोशिश करें",
            Toast.LENGTH_SHORT,
        ).show()
        finishWith(source)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResumeBuilderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        data = ResumeStore.load(this) ?: ResumeData()
        bindStepOne()
        bindStepTwo()
        bindStepThree()
        bindStepFour()
        prefillFromProfile()

        binding.resumeBack.setOnClickListener { goBack() }
        binding.resumePrevious.setOnClickListener { goBack() }
        binding.resumeNext.setOnClickListener { goNext() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goBack()
        })

        showStep(1)
    }

    override fun onPause() {
        super.onPause()
        readFields()
        ResumeStore.save(this, data)
    }

    // ------------------------------------------------------------- navigation

    private fun goNext() {
        readFields()
        if (step < STEPS) {
            val missing = ResumeRules.missing(step, data)
            if (missing.isNotEmpty()) {
                AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
                    .setTitle("पहले यह भरें")
                    .setMessage(missing.joinToString("\n") { "• $it" })
                    .setPositiveButton("ठीक है", null)
                    .show()
                return
            }
            ResumeStore.save(this, data)
            showStep(step + 1)
        } else {
            saveFinalPdf()
        }
    }

    private fun goBack() {
        readFields()
        ResumeStore.save(this, data)
        if (step > 1) showStep(step - 1) else finish()
    }

    private fun showStep(n: Int) {
        step = n
        listOf(binding.resumeStep1, binding.resumeStep2, binding.resumeStep3, binding.resumeStep4)
            .forEachIndexed { index, view -> view.visibility = if (index == n - 1) View.VISIBLE else View.GONE }
        binding.resumeStepLabel.text = "$n / $STEPS"
        binding.resumeProgress.progress = n
        binding.resumePrevious.text = if (n == 1) "रद्द करें" else "पीछे"
        binding.resumeNext.text = if (n == STEPS) "रिज्यूमे सेव करें" else "आगे"
        if (n == STEPS) renderPreview()
    }

    // ------------------------------------------------------------- step 1

    private fun bindStepOne() {
        binding.resumeName.setText(data.name)
        binding.resumeMobile.setText(data.mobile)
        binding.resumeEmail.setText(data.email)
        binding.resumeDistrict.setText(data.district)
        binding.resumeAddress.setText(data.address)
        showDob()
        showPhoto()
        binding.resumePhotoButton.setOnClickListener { photoPicker.launch("image/*") }
        binding.resumePhoto.setOnClickListener { photoPicker.launch("image/*") }
        binding.resumeSelfieButton.setOnClickListener { takeSelfie() }
        binding.resumeDob.setOnClickListener { pickDob() }
    }

    private fun readFields() {
        data = data.copy(
            name = binding.resumeName.text.toString().trim(),
            mobile = binding.resumeMobile.text.toString().filter { it.isDigit() },
            email = binding.resumeEmail.text.toString().trim(),
            district = binding.resumeDistrict.text.toString().trim(),
            address = binding.resumeAddress.text.toString().trim(),
        )
    }

    private fun showDob() {
        binding.resumeDob.text = if (data.dob.isBlank()) "" else {
            val age = ResumeRules.ageOn(data.dob)?.let { "  (उम्र $it)" }.orEmpty()
            ResumeCopy.date(data.dob, ResumeLanguage.HINDI) + age
        }
    }

    private fun pickDob() {
        val parts = data.dob.split("-").mapNotNull { it.toIntOrNull() }
        val year = parts.getOrNull(0) ?: 2000
        val month = (parts.getOrNull(1) ?: 1) - 1
        val day = parts.getOrNull(2) ?: 1
        val dialog = DatePickerDialog(this, { _, y, m, d ->
            data = data.copy(dob = "%04d-%02d-%02d".format(y, m + 1, d))
            showDob()
        }, year, month, day)
        val latest = Calendar.getInstance().apply { add(Calendar.YEAR, -ResumeRules.MIN_AGE) }
        val earliest = Calendar.getInstance().apply { add(Calendar.YEAR, -ResumeRules.MAX_AGE) }
        dialog.datePicker.maxDate = latest.timeInMillis
        dialog.datePicker.minDate = earliest.timeInMillis
        dialog.show()
    }

    private fun savePhoto(uri: Uri) {
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    val bitmap = decodeUpright(uri, 900) ?: return@runCatching null
                    val file = ResumeStore.photoFile(this@ResumeBuilderActivity)
                    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
                    file.absolutePath
                }.getOrNull()
            }
            if (saved == null) {
                Toast.makeText(this@ResumeBuilderActivity, "यह फोटो नहीं खुल पाई, कोई और चुनें", Toast.LENGTH_SHORT).show()
                return@launch
            }
            data = data.copy(photoPath = saved)
            ResumeStore.save(this@ResumeBuilderActivity, data)
            showPhoto()
        }
    }

    private fun takeSelfie() {
        val file = File(cacheDir, "resume_selfie.jpg")
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        selfieUri = uri
        runCatching { selfieTaker.launch(uri) }.onFailure {
            Toast.makeText(this, "कैमरा नहीं खुल पाया, गैलरी से फोटो चुनें", Toast.LENGTH_SHORT).show()
        }
    }

    /** Decodes at most [maxSide] pixels on the long side, turned the way the camera held it. */
    private fun decodeUpright(uri: Uri, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val bitmap = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val rotation = contentResolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        if (rotation == 0f) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotation) }, true)
    }

    private fun loadPhoto(): Bitmap? =
        data.photoPath?.takeIf { File(it).exists() }?.let { BitmapFactory.decodeFile(it) }

    private fun loadLogo(): Bitmap? =
        runCatching { BitmapFactory.decodeResource(resources, R.drawable.rojgaarwaala_logo_rounded) }.getOrNull()

    private fun showPhoto() {
        val photo = loadPhoto()
        if (photo != null) {
            binding.resumePhoto.setImageBitmap(photo)
            binding.resumePhotoButton.text = "गैलरी से बदलें"
            binding.resumeSelfieButton.text = "नई सेल्फी लें"
        } else {
            binding.resumePhoto.setImageResource(R.drawable.ic_nav_profile)
            binding.resumePhotoButton.text = "गैलरी से फोटो"
            binding.resumeSelfieButton.text = "सेल्फी लें"
        }
    }

    private fun prefillFromProfile() {
        val viewModel = ViewModelProvider(this)[HomePageViewModel::class.java]
        viewModel.profileUpdateLiveData.observe(this) { result ->
            val profile = (result as? ApiResult.Success)?.data?.dataObj?.userDetails ?: return@observe
            readFields()
            // Only fills what the seeker has not typed already.
            data = data.copy(
                // New accounts get a placeholder name like "User_8151"; that is not a name to print.
                name = data.name.ifBlank { profile.name.orEmpty().takeUnless { PLACEHOLDER_NAME.matches(it) }.orEmpty() },
                mobile = data.mobile.ifBlank { profile.mobile.orEmpty().filter { it.isDigit() }.takeLast(10) },
                email = data.email.ifBlank { profile.email.orEmpty().takeUnless { it.endsWith("@temp.com") }.orEmpty() },
                // Only a real district: the profile can hold "All Chhattisgarh".
                district = data.district.ifBlank {
                    ChhattisgarhDistricts.list.firstOrNull { it.equals(profile.district.orEmpty().trim(), ignoreCase = true) }.orEmpty()
                },
                address = data.address.ifBlank { profile.address ?: profile.colony.orEmpty() },
                roles = data.roles.ifEmpty {
                    (profile.preferredJobCategories.orEmpty() + listOfNotNull(profile.preferredJobCategory))
                        .map { it.trim() }.filter { it.isNotBlank() }.distinct().take(ResumeRules.MAX_ROLES)
                },
            )
            if (data.jobLocations.isEmpty()) {
                ChhattisgarhDistricts.list.firstOrNull { it.equals(data.district, ignoreCase = true) }
                    ?.let { data = data.copy(jobLocations = listOf(it)) }
            }
            bindStepOne()
            renderRoles()
            renderJobLocations()
        }
        viewModel.getProfileData()
    }

    // ------------------------------------------------------------- step 2

    private fun renderRoles() {
        val group = binding.resumeRoles
        group.removeAllViews()
        data.roles.forEach { role ->
            group.addView(baseChip(role).apply {
                isCloseIconVisible = true
                closeIconTint = ColorStateList.valueOf(Color.WHITE)
                setOnCloseIconClickListener {
                    data = data.copy(roles = data.roles - role)
                    renderRoles()
                }
            })
        }
        if (data.roles.size < ResumeRules.MAX_ROLES) {
            group.addView(actionChip("+ काम जोड़ें") { addRole() })
        }
    }

    private fun addRole() {
        val input = field("जैसे: ड्राइवर, हेल्पर, अकाउंटेंट")
        AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle("कौन सा काम चाहिए")
            .setView(column(input))
            .setPositiveButton("जोड़ें") { _, _ ->
                val typed = input.text.toString().split(",").map { it.trim() }.filter { it.isNotBlank() }
                val merged = (data.roles + typed).distinctBy { it.lowercase() }.take(ResumeRules.MAX_ROLES)
                data = data.copy(roles = merged)
                renderRoles()
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun renderJobLocations() {
        val keys = listOf(ResumeCopy.ANYWHERE) + ChhattisgarhDistricts.list
        fillChips(binding.resumeJobLocations, keys, data.jobLocations) { selected ->
            data = data.copy(jobLocations = selected)
        }
    }

    private fun bindStepTwo() {
        renderRoles()
        renderJobLocations()
        binding.resumeFresher.isChecked = data.fresher
        binding.resumeFresher.setOnCheckedChangeListener { _, checked ->
            data = data.copy(fresher = checked)
            renderExperiences()
        }
        binding.resumeAddExperience.setOnClickListener { addExperience() }
        renderExperiences()
    }

    private fun renderExperiences() {
        val list = binding.resumeExperienceList
        list.removeAllViews()
        data.experiences.forEachIndexed { index, e ->
            val line = listOf(e.role, e.company, ResumeCopy.duration(e.years, ResumeLanguage.HINDI))
                .filter { it.isNotBlank() }.joinToString("  •  ")
            list.addView(rowView(line) {
                data = data.copy(experiences = data.experiences.filterIndexed { i, _ -> i != index })
                renderExperiences()
            })
        }
        binding.resumeAddExperience.alpha = if (data.fresher) 0.5f else 1f
    }

    private fun addExperience() {
        if (data.fresher) {
            Toast.makeText(this, "पहले \"फ्रेशर\" का निशान हटाएँ", Toast.LENGTH_SHORT).show()
            return
        }
        if (data.experiences.size >= ResumeRules.MAX_EXPERIENCES) {
            Toast.makeText(this, "एक पेज में ${ResumeRules.MAX_EXPERIENCES} काम तक आते हैं, पुराना हटाएँ", Toast.LENGTH_SHORT).show()
            return
        }
        val role = field("काम (जैसे: हेल्पर, ड्राइवर)")
        val company = field("कंपनी / दुकान का नाम")
        val years = field("कितने साल (0 अगर 1 साल से कम)").apply { inputType = InputType.TYPE_CLASS_NUMBER }
        AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle("अनुभव जोड़ें")
            .setView(column(role, company, years))
            .setPositiveButton("जोड़ें") { _, _ ->
                val r = role.text.toString().trim()
                if (r.isEmpty()) {
                    Toast.makeText(this, "काम का नाम लिखें", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val y = years.text.toString().toIntOrNull()?.coerceIn(0, 50) ?: 0
                data = data.copy(experiences = data.experiences + Experience(r, company.text.toString().trim(), y))
                renderExperiences()
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    // ------------------------------------------------------------- step 3

    private fun bindStepThree() {
        binding.resumeAddEducation.setOnClickListener { addEducation() }
        renderEducation()

        val customSkills = data.skills.filter { it !in ResumeCopy.SKILLS }
        fillChips(binding.resumeSkills, ResumeCopy.SKILLS + customSkills, data.skills) { selected ->
            data = data.copy(skills = selected)
        }
        binding.resumeSkills.addView(actionChip("+ अपना हुनर") { addCustomSkill() })
        fillChips(binding.resumeLanguages, ResumeCopy.LANGUAGES, data.languages) { data = data.copy(languages = it) }
        fillChips(binding.resumeLicences, ResumeCopy.LICENCES, data.licences) { data = data.copy(licences = it) }
    }

    private fun renderEducation() {
        val list = binding.resumeEducationList
        list.removeAllViews()
        data.education.forEachIndexed { index, e ->
            val line = listOf(
                ResumeCopy.label(e.level, ResumeLanguage.HINDI),
                e.institute,
                if (e.year > 0) e.year.toString() else "",
            ).filter { it.isNotBlank() }.joinToString("  •  ")
            list.addView(rowView(line) {
                data = data.copy(education = data.education.filterIndexed { i, _ -> i != index })
                renderEducation()
            })
        }
    }

    private fun addEducation() {
        if (data.education.size >= ResumeRules.MAX_EDUCATION) {
            Toast.makeText(this, "एक पेज में ${ResumeRules.MAX_EDUCATION} पढ़ाई तक आती हैं, कोई हटाएँ", Toast.LENGTH_SHORT).show()
            return
        }
        val levels = ResumeCopy.EDUCATION_LEVELS
        val levelSpinner = spinner(levels.map { ResumeCopy.label(it, ResumeLanguage.HINDI) })
        val institute = field("स्कूल / कॉलेज / बोर्ड (ज़रूरी नहीं)")
        val years = ResumeRules.passingYears()
        val yearSpinner = spinner(listOf("पास होने का साल चुनें") + years.map { it.toString() })
        AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle("पढ़ाई जोड़ें")
            .setView(column(label("कक्षा / डिग्री"), levelSpinner, institute, label("पास होने का साल"), yearSpinner))
            .setPositiveButton("जोड़ें") { _, _ ->
                val yearIndex = yearSpinner.selectedItemPosition
                if (yearIndex == 0) {
                    Toast.makeText(this, "पास होने का साल चुनें", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val entry = Education(
                    level = levels[levelSpinner.selectedItemPosition],
                    institute = institute.text.toString().trim(),
                    year = years[yearIndex - 1],
                )
                data = data.copy(education = data.education + entry)
                renderEducation()
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun addCustomSkill() {
        val input = field("जैसे: फोर्कलिफ्ट चलाना")
        AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle("अपना हुनर जोड़ें")
            .setView(column(input))
            .setPositiveButton("जोड़ें") { _, _ ->
                val skill = input.text.toString().trim()
                if (skill.isEmpty() || skill in data.skills) return@setPositiveButton
                if (data.skills.size >= ResumeRules.MAX_SKILLS) {
                    Toast.makeText(this, "${ResumeRules.MAX_SKILLS} हुनर तक चुन सकते हैं", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                data = data.copy(skills = data.skills + skill)
                val group = binding.resumeSkills
                group.removeAllViews()
                bindStepThreeChipsOnly()
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun bindStepThreeChipsOnly() {
        val customSkills = data.skills.filter { it !in ResumeCopy.SKILLS }
        fillChips(binding.resumeSkills, ResumeCopy.SKILLS + customSkills, data.skills) { selected ->
            data = data.copy(skills = selected)
        }
        binding.resumeSkills.addView(actionChip("+ अपना हुनर") { addCustomSkill() })
    }

    // ------------------------------------------------------------- step 4

    private fun bindStepFour() {
        val languages = listOf(ResumeLanguage.HINDI to "हिंदी", ResumeLanguage.ENGLISH to "English")
        languages.forEach { (value, title) ->
            binding.resumeLanguageChoice.addView(choiceChip(title, data.language == value) {
                data = data.copy(language = value)
                renderPreview()
            })
        }
    }

    /** Draws the real PDF and shows it, so what is seen is exactly what is sent. */
    private fun renderPreview() {
        previewJob?.cancel()
        binding.resumeWorking.visibility = View.VISIBLE
        val snapshot = data
        val width = resources.displayMetrics.widthPixels - (32 * resources.displayMetrics.density).toInt()
        previewJob = lifecycleScope.launch {
            val pages = withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(cacheDir, "resume_preview.pdf")
                    ResumePdfWriter.write(snapshot, loadPhoto(), file, loadLogo())
                    renderPages(file, width)
                }.getOrDefault(emptyList())
            }
            binding.resumeWorking.visibility = View.GONE
            val holder = binding.resumePreviewPages
            holder.removeAllViews()
            pages.forEach { bitmap ->
                holder.addView(ImageView(this@ResumeBuilderActivity).apply {
                    setImageBitmap(bitmap)
                    adjustViewBounds = true
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { bottomMargin = (10 * resources.displayMetrics.density).toInt() }
                })
            }
            if (pages.isEmpty()) {
                Toast.makeText(this@ResumeBuilderActivity, "रिज्यूमे नहीं बन पाया, दोबारा कोशिश करें", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderPages(file: File, width: Int): List<Bitmap> {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                return (0 until renderer.pageCount).map { index ->
                    renderer.openPage(index).use { page ->
                        val height = (width.toFloat() * page.height / page.width).toInt()
                        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                            it.eraseColor(Color.WHITE)
                            page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                }
            }
        }
    }

    private fun saveFinalPdf() {
        binding.resumeNext.isEnabled = false
        binding.resumeWorking.visibility = View.VISIBLE
        val snapshot = data
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching {
                    ResumeStore.save(this@ResumeBuilderActivity, snapshot)
                    ResumeStore.pdfFile(this@ResumeBuilderActivity, snapshot).also {
                        ResumePdfWriter.write(snapshot, loadPhoto(), it, loadLogo())
                    }
                }.getOrNull()
            }
            binding.resumeNext.isEnabled = true
            binding.resumeWorking.visibility = View.GONE
            if (file == null) {
                Toast.makeText(this@ResumeBuilderActivity, "रिज्यूमे सेव नहीं हो पाया, दोबारा कोशिश करें", Toast.LENGTH_SHORT).show()
                return@launch
            }
            AlertDialog.Builder(this@ResumeBuilderActivity, R.style.Theme_Rojgaarwaala_AlertDialog)
                .setTitle("रिज्यूमे तैयार है")
                .setMessage("फोन में डाउनलोड करके कहीं भी भेज सकते हैं, या सीधे WhatsApp पर शेयर करें।")
                .setPositiveButton("ठीक है") { _, _ -> finishWith(file) }
                .setNegativeButton("डाउनलोड करें") { _, _ ->
                    pendingDownload = file
                    downloader.launch(file.name)
                }
                .setNeutralButton("शेयर करें") { _, _ ->
                    share(file)
                    finishWith(file)
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun finishWith(file: File) {
        setResult(RESULT_OK, Intent().putExtra(EXTRA_PDF_PATH, file.absolutePath))
        finish()
    }

    private fun share(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, "रिज्यूमे भेजें"))
    }

    // ------------------------------------------------------------- small views

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun rowView(text: String, onRemove: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundResource(R.drawable.modern_edit_text_background)
        setPadding(dp(14), dp(12), dp(6), dp(12))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            .apply { bottomMargin = dp(8) }
        addView(TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        addView(TextView(context).apply {
            this.text = "हटाएँ"
            setTextColor(ContextCompat.getColor(context, R.color.brand_color_red))
            textSize = 13f
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { onRemove() }
        })
    }

    private fun field(hint: String): EditText = EditText(this).apply {
        this.hint = hint
        setTextColor(Color.WHITE)
        setHintTextColor(ContextCompat.getColor(context, R.color.search_hint))
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
    }

    private fun label(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(ContextCompat.getColor(context, R.color.search_hint))
        textSize = 13f
        setPadding(0, dp(10), 0, dp(2))
    }

    private fun spinner(items: List<String>): Spinner = Spinner(this).apply {
        adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, items)
    }

    private fun column(vararg views: View): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(8), dp(20), 0)
        views.forEach { addView(it) }
    }

    private fun chipColors(): Pair<ColorStateList, ColorStateList> {
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        val amber = ContextCompat.getColor(this, R.color.brand_color_yellow)
        val onBrand = ContextCompat.getColor(this, R.color.on_brand)
        return ColorStateList(states, intArrayOf(amber, Color.parseColor("#1C1C1C"))) to
            ColorStateList(states, intArrayOf(onBrand, Color.WHITE))
    }

    private fun baseChip(text: String): Chip = Chip(this).apply {
        this.text = text
        val (bg, fg) = chipColors()
        chipBackgroundColor = bg
        setTextColor(fg)
        chipStrokeWidth = dp(1).toFloat()
        chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#444444"))
        isCheckedIconVisible = false
    }

    private fun fillChips(group: ChipGroup, keys: List<String>, selected: List<String>, onChange: (List<String>) -> Unit) {
        group.removeAllViews()
        val chosen = selected.toMutableList()
        keys.forEach { key ->
            group.addView(baseChip(ResumeCopy.label(key, ResumeLanguage.HINDI)).apply {
                isCheckable = true
                isChecked = key in chosen
                setOnCheckedChangeListener { _, checked ->
                    if (checked) { if (key !in chosen) chosen += key } else chosen -= key
                    onChange(chosen.toList())
                }
            })
        }
    }

    private fun actionChip(text: String, onClick: () -> Unit): Chip = baseChip(text).apply {
        isCheckable = false
        setTextColor(ContextCompat.getColor(context, R.color.brand_color_yellow))
        setOnClickListener { onClick() }
    }

    private fun choiceChip(text: String, checked: Boolean, onPick: () -> Unit): Chip = baseChip(text).apply {
        isCheckable = true
        isChecked = checked
        setOnCheckedChangeListener { _, isNowChecked -> if (isNowChecked) onPick() }
    }
}
