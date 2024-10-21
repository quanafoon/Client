package dev.kwasi.echoservercomplete.network

import android.util.Log
import com.google.gson.Gson
import dev.kwasi.echoservercomplete.models.ContentModel
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.Exception
import kotlin.concurrent.thread

/// The [Server] class has all the functionality that is responsible for the 'server' connection.
/// This is implemented using TCP. This Server class is intended to be run on the GO.

class Server(private val iFaceImpl:NetworkMessageInterface) {
    companion object {
        const val PORT: Int = 9999

    }

    private lateinit var svrSocket: ServerSocket
    private val clientMap: HashMap<String, Socket> = HashMap()
    private var serverThread : Thread? = null
    private var handleThread: Thread? = null
    @Volatile
    private var isRunning = true

    init {
        isRunning=true
        clientMap.clear()
        iFaceImpl.onStudentsUpdated(emptyList())
       // studentMessages.clear()
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


    private fun handleSocket(socket: Socket){
        socket.inetAddress.hostAddress?.let {
            clientMap["Bob"] = socket
            iFaceImpl.onStudentsUpdated(clientMap.keys.toList())
            iFaceImpl.onStudentConnected(socket.inetAddress?.hostAddress.toString())
            Log.e("SERVER", "A new connection has been detected!")
            handleThread = thread {

                while(!socket.isClosed){
                    try{
                        listenForMessages(socket)
                    } catch (e: Exception){
                        Log.e("SERVER", "An error has occurred with the client $it")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun sendMessage(content: ContentModel){
        thread{
            val writer = clientMap["Bob"]?.getOutputStream()?.bufferedWriter()
            val contentAsStr:String = Gson().toJson(content)
            writer?.write("$contentAsStr\n")
            writer?.flush()
        }
    }

    private fun listenForMessages(clientSocket: Socket) {
        val reader = clientSocket.getInputStream().bufferedReader()
            try {
                while (!clientSocket.isClosed) {
                    val receivedMessage = reader.readLine()
                    if (receivedMessage != null) {
                        Log.e("Client", "Received: $receivedMessage")
                        val message =Gson().fromJson(receivedMessage, ContentModel::class.java)
                        iFaceImpl.onContent(message)
                    }
                }
            } catch (e: Exception) {
                Log.e("Client", "Error receiving message: ${e.message}")
            }
    }



    fun close() {
        isRunning = false
        handleThread?.interrupt()
        serverThread?.interrupt()
        clientMap.values.forEach {
            try {
                it.close()
            } catch (e: Exception) {
                Log.d("Server", "Error Closing Socket ${e.message}")
            }
            svrSocket.close()
            clientMap.clear()
        }
    }

}