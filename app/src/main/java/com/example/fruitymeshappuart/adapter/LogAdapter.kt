package com.example.fruitymeshappuart.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fruitymeshappuart.R
import com.example.fruitymeshappuart.databinding.LogItemBinding

class LogAdapter :
    RecyclerView.Adapter<LogAdapter.LogViewHolder>() {
    private val logBuffer: MutableList<String> = mutableListOf()

    fun pushLog(logContent: String) {
        if (logBuffer.size > LOG_MAX_BUFFER) {
            logBuffer.removeFirst()
        }
        logBuffer.add(logContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.log_item, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind.content.text = logBuffer[position]
        holder.bind.line.text = (position + 1).toString()
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