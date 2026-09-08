package com.example.data.model

data class PermissionCategory(
    val categoryId: String,
    val titleAr: String,
    val items: List<PermissionItem>
)

data class PermissionItem(
    val key: String,
    val titleAr: String,
    val descriptionAr: String = ""
)

object PermissionCatalog {

    // Category 1: Customers
    const val CUSTOMERS_VIEW = "customers_view"
    const val CUSTOMERS_ADD = "customers_add"
    const val CUSTOMERS_EDIT = "customers_edit"
    const val CUSTOMERS_DELETE = "customers_delete"

    // Category 2: Meters
    const val METERS_VIEW = "meters_view"
    const val METERS_ADD = "meters_add"
    const val METERS_EDIT = "meters_edit"
    const val METERS_DELETE = "meters_delete"

    // Category 3: Readings
    const val READINGS_ADD = "readings_add"
    const val READINGS_EDIT = "readings_edit"
    const val READINGS_DELETE = "readings_delete"
    const val READINGS_APPROVE = "readings_approve"

    // Category 4: Invoices
    const val INVOICES_VIEW = "invoices_view"
    const val INVOICES_CREATE = "invoices_create"
    const val INVOICES_EDIT = "invoices_edit"
    const val INVOICES_DELETE = "invoices_delete"
    const val INVOICES_PRINT = "invoices_print"

    // Category 5: Payments
    const val PAYMENTS_COLLECT = "payments_collect"
    const val PAYMENTS_EDIT = "payments_edit"
    const val PAYMENTS_DELETE = "payments_delete"
    const val PAYMENTS_HISTORY = "payments_history"

    // Category 6: Reports
    const val REPORTS_VIEW = "reports_view"
    const val REPORTS_EXPORT_PDF = "reports_export_pdf"
    const val REPORTS_EXPORT_EXCEL = "reports_export_excel"
    const val REPORTS_PRINT = "reports_print"

    // Category 7: Users & Keys
    const val KEYS_VIEW = "keys_view"
    const val KEYS_CREATE = "keys_create"
    const val KEYS_EDIT = "keys_edit"
    const val KEYS_DELETE = "keys_delete"
    const val KEYS_DISABLE = "keys_disable"

    // Category 8: Settings
    const val SETTINGS_EDIT = "settings_edit"
    const val TARIFF_EDIT = "tariff_edit"
    const val COMPANY_EDIT = "company_edit"
    const val BACKUP_CREATE = "backup_create"
    const val BACKUP_RESTORE = "backup_restore"

    // Category 9: Additional Permissions
    const val DATA_SYNC = "data_sync"
    const val DATA_WIPE = "data_wipe"
    const val DATA_IMPORT = "data_import"
    const val DATA_EXPORT = "data_export"
    const val AUDIT_LOG = "audit_log"

    // Legacy key mappings for backward compatibility with existing screen components
    const val LEGACY_CAN_ADD_BILL = "canAddBill"
    const val LEGACY_CAN_PAY_BILL = "canPayBill"
    const val LEGACY_CAN_MANAGE_USERS = "canManageUsers"
    const val LEGACY_CAN_VIEW_REPORTS = "canViewReports"

    val allCategories = listOf(
        PermissionCategory(
            categoryId = "customers",
            titleAr = "العملاء",
            items = listOf(
                PermissionItem(CUSTOMERS_VIEW, "عرض العملاء", "سماح بستعرض قائمة المشتركين والعملاء"),
                PermissionItem(CUSTOMERS_ADD, "إضافة عميل", "سماح بتسجيل مشترك جديد"),
                PermissionItem(CUSTOMERS_EDIT, "تعديل عميل", "تعديل بيانات المشترك والعنوان"),
                PermissionItem(CUSTOMERS_DELETE, "حذف عميل", "حذف حساب المشترك")
            )
        ),
        PermissionCategory(
            categoryId = "meters",
            titleAr = "العدادات",
            items = listOf(
                PermissionItem(METERS_VIEW, "عرض العدادات", "استعراض العدادات المربوطة"),
                PermissionItem(METERS_ADD, "إضافة عداد", "ربط عداد جديد بحساب مشترك"),
                PermissionItem(METERS_EDIT, "تعديل عداد", "تعديل رقم ومعامل العداد"),
                PermissionItem(METERS_DELETE, "حذف عداد", "إلغاء ربط وإزالة عداد")
            )
        ),
        PermissionCategory(
            categoryId = "readings",
            titleAr = "القراءات",
            items = listOf(
                PermissionItem(READINGS_ADD, "تسجيل قراءة", "إدخال قراءات العدادات الميدانية"),
                PermissionItem(READINGS_EDIT, "تعديل قراءة", "تصحيح قراءة سابقة"),
                PermissionItem(READINGS_DELETE, "حذف قراءة", "حذف قراءة خاطئة"),
                PermissionItem(READINGS_APPROVE, "اعتماد القراءة", "اعتماد القراءات الميدانية للفواتير")
            )
        ),
        PermissionCategory(
            categoryId = "invoices",
            titleAr = "الفواتير",
            items = listOf(
                PermissionItem(INVOICES_VIEW, "عرض الفواتير", "استعراض كشف واستعلام الفواتير"),
                PermissionItem(INVOICES_CREATE, "إنشاء فاتورة", "تصدير فاتورة جديدة لمشترك"),
                PermissionItem(INVOICES_EDIT, "تعديل فاتورة", "تعديل مبالغ وقيم الفاتورة"),
                PermissionItem(INVOICES_DELETE, "حذف فاتورة", "إلغاء وحذف الفاتورة"),
                PermissionItem(INVOICES_PRINT, "طباعة فاتورة", "طباعة الفاتورة حرارياً أو PDF")
            )
        ),
        PermissionCategory(
            categoryId = "payments",
            titleAr = "المدفوعات",
            items = listOf(
                PermissionItem(PAYMENTS_COLLECT, "تحصيل دفعة", "قبول وتسديد فواتير المشتركين"),
                PermissionItem(PAYMENTS_EDIT, "تعديل دفعة", "تعديل سند تحصيل مقبوضات"),
                PermissionItem(PAYMENTS_DELETE, "حذف دفعة", "إلغاء سند تحصيل"),
                PermissionItem(PAYMENTS_HISTORY, "عرض سجل المدفوعات", "مراجعة كشف الصندوق والمقبوضات")
            )
        ),
        PermissionCategory(
            categoryId = "reports",
            titleAr = "التقارير",
            items = listOf(
                PermissionItem(REPORTS_VIEW, "عرض التقارير", "استعراض تقارير الأداء والمبيعات"),
                PermissionItem(REPORTS_EXPORT_PDF, "تصدير PDF", "تصدير التقارير بصيغة PDF"),
                PermissionItem(REPORTS_EXPORT_EXCEL, "تصدير Excel", "تصدير البيانات بصيغة اكسل"),
                PermissionItem(REPORTS_PRINT, "طباعة التقارير", "طباعة التقارير الإحصائية")
            )
        ),
        PermissionCategory(
            categoryId = "keys_users",
            titleAr = "المستخدمون والمفاتيح",
            items = listOf(
                PermissionItem(KEYS_VIEW, "عرض المفاتيح والمستخدمين", "رؤية قائمة المفاتيح والموظفين"),
                PermissionItem(KEYS_CREATE, "إنشاء مفاتيح", "توليد مفاتيح سرية جديدة للموظفين"),
                PermissionItem(KEYS_EDIT, "تعديل المفاتيح", "تعديل صلاحيات وتأريخ صلاحية المفتاح"),
                PermissionItem(KEYS_DELETE, "حذف المفاتيح", "حذف مفتاح سري نهائياً"),
                PermissionItem(KEYS_DISABLE, "تعطيل/تفعيل المفاتيح", "تجميد وتفعيل الوصول للموظف")
            )
        ),
        PermissionCategory(
            categoryId = "settings",
            titleAr = "الإعدادات",
            items = listOf(
                PermissionItem(SETTINGS_EDIT, "تعديل إعدادات النظام", "إدارة ضبط النظام العام"),
                PermissionItem(TARIFF_EDIT, "تعديل تعرفة الكهرباء", "تحديث سعر الكيلوواط والشريحة"),
                PermissionItem(COMPANY_EDIT, "تعديل بيانات الشركة", "تحديث اسم وشعار ورأس الفاتورة"),
                PermissionItem(BACKUP_CREATE, "النسخ الاحتياطي", "إنشاء نسخة احتياطية من البيانات"),
                PermissionItem(BACKUP_RESTORE, "استعادة النسخة الاحتياطية", "استرجاع النسخة المحفوظة")
            )
        ),
        PermissionCategory(
            categoryId = "additional",
            titleAr = "صلاحيات إضافية",
            items = listOf(
                PermissionItem(DATA_SYNC, "مزامنة البيانات", "إجراء مزامنة يدوية مع السحابة"),
                PermissionItem(DATA_WIPE, "حذف جميع البيانات", "مسح وضبط المصنع"),
                PermissionItem(DATA_IMPORT, "استيراد البيانات", "استيراد ملفات خارجية"),
                PermissionItem(DATA_EXPORT, "تصدير البيانات", "تصدير قاعدة البيانات كاملة"),
                PermissionItem(AUDIT_LOG, "سجل العمليات", "مراجعة كشف حركة العمليات والتغييرات")
            )
        )
    )

    fun getAllPermissionKeys(): List<String> {
        return allCategories.flatMap { cat -> cat.items.map { it.key } }
    }

    fun getDefaultAdminPermissions(): List<String> {
        return getAllPermissionKeys()
    }

    fun getDefaultCollectorPermissions(): List<String> {
        return listOf(
            CUSTOMERS_VIEW,
            METERS_VIEW,
            READINGS_ADD,
            INVOICES_VIEW,
            INVOICES_CREATE,
            INVOICES_PRINT,
            PAYMENTS_COLLECT,
            PAYMENTS_HISTORY,
            LEGACY_CAN_ADD_BILL,
            LEGACY_CAN_PAY_BILL
        )
    }

    fun getDefaultAccountantPermissions(): List<String> {
        return listOf(
            CUSTOMERS_VIEW,
            INVOICES_VIEW,
            INVOICES_CREATE,
            INVOICES_PRINT,
            PAYMENTS_COLLECT,
            PAYMENTS_EDIT,
            PAYMENTS_HISTORY,
            REPORTS_VIEW,
            REPORTS_EXPORT_PDF,
            REPORTS_EXPORT_EXCEL,
            REPORTS_PRINT,
            LEGACY_CAN_PAY_BILL,
            LEGACY_CAN_VIEW_REPORTS
        )
    }
}
