package com.cloner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val apps: List<AppInfo>,
    private val onClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.VH>() {
    
    override fun onCreateViewHolder(p: ViewGroup, t: Int): VH {
        val v = LayoutInflater.from(p.context).inflate(android.R.layout.simple_list_item_2, p, false)
        return VH(v)
    }
    override fun onBindViewHolder(h: VH, i: Int) {
        val a = apps[i]
        h.t1.text = a.name
        h.t2.text = "${a.packageName} (${a.version})"
        h.itemView.setOnClickListener { onClick(a) }
    }
    override fun getItemCount() = apps.size
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val t1 = v.findViewById<TextView>(android.R.id.text1)!!
        val t2 = v.findViewById<TextView>(android.R.id.text2)!!
    }
}
