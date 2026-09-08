# رفع مشروع محطة العسل وبناء APK

هذا المشروع مجهز للعمل مع GitHub Actions.

بعد رفع الملفات إلى مستودع GitHub والضغط على **Actions**، سيعمل Workflow باسم **Build Android APK** تلقائيًا عند الدفع إلى فرع `main`، ويمكن تشغيله يدويًا أيضًا.

ينشئ الملف:

`app/build/outputs/apk/debug/app-debug.apk`

ويضعه في Artifacts باسم:

`alasal-billing-debug-apk`

## المتطلبات
- JDK 17
- Gradle 9.3.1
- Android SDK / Build Tools التي يوفرها GitHub runner

لا توجد حاجة إلى Gradle Wrapper في هذه النسخة لأن GitHub Actions يقوم بتثبيت Gradle 9.3.1 مباشرة.
