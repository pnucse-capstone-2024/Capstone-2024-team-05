import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.safedrive.R

class CardNewsAdapter(
    private val title: String, // 고정된 제목
    private val newsList: List<Triple<String, Int, String>> // 소제목과 내용의 리스트
) : RecyclerView.Adapter<CardNewsAdapter.CardNewsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardNewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card_news, parent, false)
        return CardNewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardNewsViewHolder, position: Int) {
        val (subtitle, image, content) = newsList[position]
        holder.bind(title, subtitle, image, content)
    }

    override fun getItemCount(): Int = newsList.size

    class CardNewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(title: String, subtitle: String, image: Int, content: String) {
            val tvCardNewsTitle = itemView.findViewById<TextView>(R.id.tvCardNewsTitle)
            val tvCardNewsSubtitle = itemView.findViewById<TextView>(R.id.tvCardNewsSubtitle)
            val ivCardNewsImage = itemView.findViewById<ImageView>(R.id.ivCardNewsImage)
            val tvCardNewsContent = itemView.findViewById<TextView>(R.id.tvCardNewsContent)

            tvCardNewsTitle.text = title
            tvCardNewsSubtitle.text = subtitle
            ivCardNewsImage.setImageResource(image)
            tvCardNewsContent.text = content
        }
    }
}