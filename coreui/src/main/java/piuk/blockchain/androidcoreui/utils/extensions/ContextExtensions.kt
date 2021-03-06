@file:JvmName("ContextExtensions")

package piuk.blockchain.androidcoreui.utils.extensions

import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

/**
 * Shows a [ToastCustom] from a given [Activity]. By default, the Toast is of type
 * [ToastCustom.TYPE_GENERAL] but can be overloaded if needed.
 *
 * @param text The text to display, as a [String]
 * @param type An optional [ToastCustom.ToastType] which can be omitted for general Toasts
 */
fun AppCompatActivity.toast(text: String, @ToastCustom.ToastType type: String = ToastCustom.TYPE_GENERAL) {
    ToastCustom.makeText(this, text, ToastCustom.LENGTH_SHORT, type)
}

/**
 * Shows a [ToastCustom] from a given [Activity]. By default, the Toast is of type
 * [ToastCustom.TYPE_GENERAL] but can be overloaded if needed.
 *
 * @param text The text to display, as a String resource [Int]
 * @param type An optional [ToastCustom.ToastType] which can be omitted for general Toasts
 */
fun AppCompatActivity.toast(@StringRes text: Int, @ToastCustom.ToastType type: String = ToastCustom.TYPE_GENERAL) {
    ToastCustom.makeText(this, getString(text), ToastCustom.LENGTH_SHORT, type)
}

/**
 * Shows a [ToastCustom] from a given [Fragment]. By default, the Toast is of type
 * [ToastCustom.TYPE_GENERAL] but can be overloaded if needed.
 *
 * @param text The text to display, as a [String]
 * @param type An optional [ToastCustom.ToastType] which can be omitted for general Toasts
 */
fun Fragment.toast(text: String, @ToastCustom.ToastType type: String = ToastCustom.TYPE_GENERAL) {
    ToastCustom.makeText(activity, text, ToastCustom.LENGTH_SHORT, type)
}

/**
 * Shows a [ToastCustom] from a given [Fragment]. By default, the Toast is of type
 * [ToastCustom.TYPE_GENERAL] but can be overloaded if needed.
 *
 * @param text The text to display, as a String resource [Int]
 * @param type An optional [ToastCustom.ToastType] which can be omitted for general Toasts
 */
fun Fragment.toast(@StringRes text: Int, @ToastCustom.ToastType type: String = ToastCustom.TYPE_GENERAL) {
    ToastCustom.makeText(activity, getString(text), ToastCustom.LENGTH_SHORT, type)
}

/**
 * Shows a [ToastCustom] from a given [Context]. By default, the Toast is of type
 * [ToastCustom.TYPE_GENERAL] but can be overloaded if needed. Be careful not to abuse this an
 * call [toast] from an Application Context.
 *
 * @param text The text to display, as a [String]
 * @param type An optional [ToastCustom.ToastType] which can be omitted for general Toasts
 */
fun Context.toast(text: String, @ToastCustom.ToastType type: String = ToastCustom.TYPE_GENERAL) {
    ToastCustom.makeText(this, text, ToastCustom.LENGTH_SHORT, type)
}

/**
 * Shows a [ToastCustom] from a given [Context]. By default, the Toast is of type
 * [ToastCustom.TYPE_GENERAL] but can be overloaded if needed. Be careful not to abuse this an
 * call [toast] from an Application Context.
 *
 * @param text The text to display, as a String resource [Int]
 * @param type An optional [ToastCustom.ToastType] which can be omitted for general Toasts
 */
fun Context.toast(@StringRes text: Int, @ToastCustom.ToastType type: String = ToastCustom.TYPE_GENERAL) {
    ToastCustom.makeText(this, getString(text), ToastCustom.LENGTH_SHORT, type)
}

/**
 * Returns a color associated with a particular resource ID.
 *
 * @param color The Res ID of the color.
 */
fun Context.getResolvedColor(@ColorRes color: Int): Int = ContextCompat.getColor(this, color)

/**
 * Returns a color associated with a particular resource ID.
 *
 * @param color The Res ID of the color.
 */
fun Fragment.getResolvedColor(@ColorRes color: Int): Int =
    ContextCompat.getColor(requireContext(), color)

/**
 * Returns a nullable Drawable associated with a particular resource ID.
 *
 * @param drawable The Res ID of the Drawable.
 */
fun Context.getResolvedDrawable(@DrawableRes drawable: Int): Drawable? =
    ContextCompat.getDrawable(this, drawable)
