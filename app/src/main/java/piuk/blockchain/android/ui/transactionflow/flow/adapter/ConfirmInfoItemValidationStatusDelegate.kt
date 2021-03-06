package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_send_confirm_error_notice.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcoreui.utils.extensions.context
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import java.lang.IllegalStateException

class ConfirmInfoItemValidationStatusDelegate<in T> : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        return (items[position] as? TxOptionValue)?.option == TxOption.ERROR_NOTICE
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ViewHolder(parent.inflate(R.layout.item_send_confirm_error_notice))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ViewHolder).bind(
        items[position] as TxOptionValue.ErrorNotice
    )

    class ViewHolder(
        val parent: View
    ) : RecyclerView.ViewHolder(parent), LayoutContainer {

        override val containerView: View?
            get() = itemView

        fun bind(item: TxOptionValue.ErrorNotice) {
            itemView.error_msg.text = item.status.toText(context)
        }

        // By the time we are on the confirmation screen most of these possible error should have been
        // filtered out. A few remain possible, because BE failures or BitPay invoices, thus:
        private fun ValidationState.toText(ctx: Context) =
            when (this) {
                ValidationState.CAN_EXECUTE -> throw IllegalStateException("Displaying OK in error status")
                ValidationState.UNINITIALISED -> throw IllegalStateException("Displaying OK in error status")
                ValidationState.INSUFFICIENT_FUNDS -> ctx.getString(R.string.confirm_status_msg_insufficient_funds)
                ValidationState.INSUFFICIENT_GAS -> ctx.getString(R.string.confirm_status_msg_insufficient_gas)
                ValidationState.OPTION_INVALID -> ctx.getString(R.string.confirm_status_msg_option_invalid)
                ValidationState.INVOICE_EXPIRED -> ctx.getString(R.string.confirm_status_msg_invoice_expired)
                else -> ctx.getString(R.string.confirm_status_msg_unexpected_error)
            }
    }
}
