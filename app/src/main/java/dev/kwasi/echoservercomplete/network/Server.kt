package dev.kwasi.echoservercomplete.network

import android.util.Log
import com.google.gson.Gson
import dev.kwasi.echoservercomplete.models.ContentModel
import dev.kwasi.echoservercomplete.student.StudentAdapterInterface
import java.net.BindException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.Exception
import kotlin.concurrent.thread

/// The [Server] class has all the functionality that is responsible for the 'server' connection.
/// This is implemented using TCP. This Server class is intended to be run on the GO.

class Server(private val connectionListener:StudentAdapterInterface) {
    companion object {
        const val PORT: Int = 9090
    }

    private var isRunning = false
    private val studentMap: HashMap<String, Socket> = HashMap()
    val studentMessages: HashMap<String, MutableList<ContentModel>> = HashMap()

    private var serverThread: Thread? = null

    private lateinit var svrSocket: ServerSocket


    init {
        startServer()
    }

    private fun startServer() {
        if (::svrSocket.isInitialized && !svrSocket.isClosed) {
            Log.e("Server", "Server is already running.")
            return
        }
        serverThread = thread {
            try {
                svrSocket = ServerSocket(PORT, 0, InetAddress.getByName("192.168.49.1"))
                Log.d("Server", "Server started on port $PORT")

                while (isRunning) {
                    try {
                        val studentSocket = svrSocket.accept()
                        Log.d("SERVER", "The server has accepted a connection from ${studentSocket.inetAddress.hostAddress}")
                        handleSocket(studentSocket)
                    } catch (e: Exception) {
                        Log.e("SERVER", "An error has occurred in the server ${e.message}! and is running is $isRunning")
                    }
                }
            } catch (e: Exception) {
                Log.e("Server", "Server Socket Error: ${e.message}")
            }
        }
    }


    private fun handleSocket(studentSocket: Socket) {
            studentSocket.inetAddress.hostAddress?.let { clientAddress ->
                Log.e("SERVER", "A new student has connected from IP: $clientAddress")
                thread {
                    try {
                        val studentReader = studentSocket.getInputStream().bufferedReader()

                        val receivedJson: String? = studentReader.readLine()
                        val studentId =
                            Gson().fromJson(receivedJson, ContentModel::class.java)
                        studentMap[studentId.message] = studentSocket

                        studentMessages[studentId.message] = mutableListOf()

                        connectionListener.onStudentConnected(studentId.message)
                        connectionListener.onStudentsUpdated(studentMap.keys.toList())

                        handleStudentMessages(studentSocket, studentId.message)
                    } catch (e: Exception) {
                            Log.e("SERVER", "An error has occurred with the client $clientAddress")
                            e.printStackTrace()
                        }
                    }
                } ?.run {
                    Log.e("Server","Failed to retrieve client IP address")
            }
        }

       private fun handleStudentMessages(studentSocket: Socket, studentId: String) {
           val studentReader = studentSocket.getInputStream().bufferedReader()
           var receivedJson: String?

           while (studentSocket.isConnected) {
               try {
                   receivedJson = studentReader.readLine()
                   if (receivedJson != null) {
                       Log.e(
                           "SERVER",
                           "Received a message from client ${studentSocket.inetAddress.hostAddress}"
                       )
                       val messageContent =
                           Gson().fromJson(receivedJson, ContentModel::class.java)

                       studentMessages[studentId]?.add(messageContent)

                       connectionListener.onMessage(
                           studentId,
                           studentMessages[studentId] ?: listOf()
                       )
                   }

               } catch (e: Exception) {
                   e.printStackTrace()
                   break
               }


           }

           studentMap.remove(studentId)
           connectionListener.onStudentsUpdated(studentMap.keys.toList())
       }

       fun sendMessage(studentId: String, content: ContentModel) {
           val studentSocket = studentMap[studentId]
           studentSocket?.let {
            thread{
                    try {
                        val contentAsStr: String = Gson().toJson(content)
                        val writer = studentSocket.getOutputStream().bufferedWriter()
                        writer.write(contentAsStr)
                        writer.newLine()
                        writer.flush()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
       }

    fun close() {

        isRunning = false
        try {
            studentMap.values.forEach {
                try {
                    it.close()
                }catch (e: Exception) {
                    Log.e("Server", "Error closing socket: ${e.message}")
                }
            }
            svrSocket.close()
            serverThread?.interrupt()
            Log.d("Server", "Server stopped and socket closed.")
        } catch (e: Exception) {
            Log.e("Server", "Error closing server socket: ${e.message}")
        }



    }

}