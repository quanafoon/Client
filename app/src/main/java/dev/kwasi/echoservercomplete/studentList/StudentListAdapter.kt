package dev.kwasi.echoservercomplete.studentList

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.kwasi.echoservercomplete.R
import dev.kwasi.echoservercomplete.network.Server


class StudentListAdapter: RecyclerView.Adapter<StudentListAdapter.ViewHolder>(){

    //takes clientMap and creates a list of present Student IDs
    private val studentList: MutableList<String> = Server.getClientMap().keys.toMutableList()


    //assigns view to textview in layout file
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val studentView: TextView = itemView.findViewById(R.id.StudentId)
    }

    //process creation of List in UI and instantiate elements
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentListAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.student_item, parent, false)
        return ViewHolder(view)
    }
    //binds text to studentId
    override fun onBindViewHolder(holder: StudentListAdapter.ViewHolder, position: Int) {
        val student = studentList[position]
        holder.studentView.text = student

    }

    override fun getItemCount(): Int {
        return studentList.size
    }

    fun updateList(newStudentList:Collection<String>) {
        studentList.clear()
        studentList.addAll(newStudentList)
        notifyDataSetChanged()
    }

}