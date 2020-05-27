package nl.tudelft.trustchain.peerchat.ui.conversation

import android.content.res.ColorStateList
import android.graphics.Color
import android.text.format.DateUtils
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_message.view.*
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.peerchat.R
import java.text.SimpleDateFormat
import java.util.*

class ChatMessageItemRenderer : ItemLayoutRenderer<ChatMessageItem, View>(
    ChatMessageItem::class.java) {
    private val dateTimeFormat = SimpleDateFormat.getDateTimeInstance()
    private val timeFormat = SimpleDateFormat.getTimeInstance()
    private val constraintSet = ConstraintSet()

    override fun bindView(item: ChatMessageItem, view: View) = with(view) {
        txtMessage.text = item.chatMessage.message
        val color = getColorByHash(context, item.chatMessage.sender.toString())
        val newColor = Color.argb(50, Color.red(color), Color.green(color), Color.blue(color))
        txtMessage.backgroundTintList = ColorStateList.valueOf(newColor)
        txtDate.text = if (DateUtils.isToday(item.chatMessage.timestamp.time))
            timeFormat.format(item.chatMessage.timestamp)
        else dateTimeFormat.format(item.chatMessage.timestamp)
        txtDate.isVisible = item.shouldShowDate
        avatar.setUser(item.chatMessage.sender.toString(), item.participantName)
        avatar.isVisible = item.shouldShowAvatar
        imgDelivered.isVisible = item.chatMessage.outgoing && item.chatMessage.ack
        constraintSet.clone(constraintLayout)
        constraintSet.removeFromHorizontalChain(txtMessage.id)
        constraintSet.removeFromHorizontalChain(bottomContainer.id)
        if (item.chatMessage.outgoing) {
            constraintSet.connect(
                txtMessage.id,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END
            )
            constraintSet.connect(
                bottomContainer.id,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END
            )
        } else {
            val avatarMargin = resources.getDimensionPixelSize(R.dimen.avatar_size) +
                resources.getDimensionPixelSize(R.dimen.avatar_margin)
            constraintSet.connect(
                txtMessage.id,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                avatarMargin
            )
            constraintSet.connect(
                bottomContainer.id,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                avatarMargin
            )
        }
        constraintSet.applyTo(constraintLayout)
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_message
    }
}
