package com.example.safedrive.ui.map

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.safedrive.R
import com.skt.tmap.poi.TMapPOIItem

class POIAdapter(
    private var poiList: List<TMapPOIItem>,
    private val onItemClick: (TMapPOIItem) -> Unit
) : RecyclerView.Adapter<POIAdapter.POIViewHolder>() {

    inner class POIViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val poiNameTextView: TextView = itemView.findViewById(R.id.poiName)
        val poiAddressTextView: TextView = itemView.findViewById(R.id.poiAddress)

        fun bind(poiItem: TMapPOIItem) {
            poiNameTextView.text = poiItem.poiName
            poiAddressTextView.text = poiItem.poiAddress
            itemView.setOnClickListener {
                onItemClick(poiItem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): POIViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_poi, parent, false)
        return POIViewHolder(view)
    }

    override fun onBindViewHolder(holder: POIViewHolder, position: Int) {
        val poiItem = poiList[position]
        Log.d("POIAdapter", "POI 아이템 바인딩: ${poiItem.poiName}")
        holder.bind(poiItem)
    }

    override fun getItemCount(): Int {
        return poiList.size
    }

    fun updatePOIList(newPoiList: List<TMapPOIItem>) {
        Log.d("POIAdapter", "POI 리스트 업데이트: ${newPoiList.size} 개의 아이템")
        poiList = newPoiList
        notifyDataSetChanged()
    }
}