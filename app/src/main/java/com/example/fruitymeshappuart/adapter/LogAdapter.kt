package com.example.fruitymeshappuart.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fruitymeshappuart.R
import com.example.fruitymeshappuart.databinding.LogItemBinding

class LogAdapter :
    RecyclerView.Adapter<LogAdapter.LogViewHolder>() {
    private val logBuffer: MutableList<Pair<String, Int>> = mutableListOf()
    private var counter = 0

    fun pushLog(logContent: String) {
        if (logBuffer.size > LOG_MAX_BUFFER) {
            logBuffer.removeFirst()
            notifyItemRemoved(0)
        }
        logBuffer.add(Pair(logContent, ++counter))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.log_item, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind.content.text = logBuffer[position].first
        holder.bind.line.text = (logBuffer[position].second).toString()
    }

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var _bind: LogItemBinding = LogItemBinding.bind(itemView)
        val bind get() = _bind
    }

    override fun getItemCount(): Int {
        return logBuffer.size
    }

    companion object {
        const val LOG_MAX_BUFFER = 200
    }

}