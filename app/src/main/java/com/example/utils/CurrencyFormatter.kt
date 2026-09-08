package com.example.utils

import java.util.Locale

/**
 * أداة تنسيق موحّدة للأرقام والمبالغ.
 *
 * سبب الإنشاء: كانت بعض النصوص تُكتب بصيغة "%,.0f" داخل نص خام (raw string)
 * دون استدعاء format() فتظهر حرفياً للمستخدم كما هي (مشكلة الإيصال)، كما أن
 * استخدام Locale الافتراضي (العربية) كان يُنتج أرقاماً هندية غير مقروءة في PDF.
 * الآن كل تنسيق يمر عبر هذه الأداة بـ Locale.US لضمان أرقام لاتينية صحيحة.
 */
object CurrencyFormatter {

    /** 12345.0 -> "12,345" */
    fun amount(value: Double): String = String.format(Locale.US, "%,.0f", value)

    /** 12345.0 -> "12,345 ريال" */
    fun riyal(value: Double): String = "${amount(value)} ريال"

    /** 12345.0 -> "12,345 ريال يمني" */
    fun riyalFull(value: Double): String = "${amount(value)} ريال يمني"

    /** 1200.0 -> "1,200" للقراءات والاستهلاك */
    fun kwh(value: Double): String = String.format(Locale.US, "%,.0f", value)
}
