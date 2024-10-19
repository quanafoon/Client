package dev.kwasi.echoservercomplete.student

import dev.kwasi.echoservercomplete.models.ContentModel

interface StudentAdapterInterface {
    fun onStudentConnected(studentAddress:String)
    fun onMessage(studentId:String , messageList: List<ContentModel>)
    fun onStudentsUpdated(students: List<String>)
}