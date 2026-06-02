package com.example.geosearch.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.geosearch.databinding.ItemPageBinding
import com.example.geosearch.model.Page

class PageAdapter(
    private val onPageClick: (Page) -> Unit,
    private val onViewClick: (Page) -> Unit,
    private val onShareClick: (Page) -> Unit,
    private val onDelClick: (Page) -> Unit
) : RecyclerView.Adapter<PageAdapter.PageViewHolder>() {

    private val pages = mutableListOf<Page>()

    fun setPages(newPages: List<Page>) {
        pages.clear()
        pages.addAll(newPages)
        notifyDataSetChanged()
    }

    fun getPages(): List<Page> = pages.toList()

    fun removePage(page: Page) {
        val index = pages.indexOf(page)
        if (index != -1) {
            pages.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size

    inner class PageViewHolder(private val binding: ItemPageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(page: Page) {
            binding.tvTitle.text = page.title

            if (page.thumbnail != null && page.thumbnail.source.isNotEmpty()) {
                Glide.with(binding.ivThumbnail.context)
                    .load(page.thumbnail.source)
                    .centerCrop()
                    .into(binding.ivThumbnail)
            } else {
                binding.ivThumbnail.setImageDrawable(null) // Or placeholder
            }

            binding.root.setOnClickListener { onPageClick(page) }
            binding.btnView.setOnClickListener { onViewClick(page) }
            binding.btnShare.setOnClickListener { onShareClick(page) }
            binding.btnDel.setOnClickListener { onDelClick(page) }
        }
    }
}
