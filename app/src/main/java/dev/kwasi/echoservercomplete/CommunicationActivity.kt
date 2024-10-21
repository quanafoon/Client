package dev.kwasi.echoservercomplete

import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.kwasi.echoservercomplete.chatlist.ChatAdapter
import dev.kwasi.echoservercomplete.models.ContentModel
import dev.kwasi.echoservercomplete.network.ConnectionListener
import dev.kwasi.echoservercomplete.network.Server
import dev.kwasi.echoservercomplete.student.StudentAdapter
import dev.kwasi.echoservercomplete.student.StudentAdapterInterface
import dev.kwasi.echoservercomplete.wifidirect.WifiDirectInterface
import dev.kwasi.echoservercomplete.wifidirect.WifiDirectManager


class CommunicationActivity :
    AppCompatActivity(), WifiDirectInterface, StudentAdapterInterface{
    private var wfdManager: WifiDirectManager? = null

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }



    private lateinit var chatAdapter: ChatAdapter
    private lateinit var studentAdapter: StudentAdapter
    private lateinit var selectedStudent: String
    private var wfdAdapterEnabled = false
    private var wfdHasConnection = false
    private var server: Server? = null
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var startClassBtn : Button
    private lateinit var endClassbtn : Button
    private lateinit var classInfoText: TextView
    private lateinit var classPasswordtext: TextView
    private lateinit var activeStudentTextView: TextView
    private var deviceIp: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.wfd_status_communication)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        registerReceiver(wfdManager, intentFilter)



        val manager: WifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = manager.initialize(this, mainLooper, null)
        wfdManager = WifiDirectManager(manager, channel, this)


        startClassBtn = findViewById(R.id.startClassBtn)

        startClassBtn.setOnClickListener{ view : View ->
            createGroup(view)
            Log.d("CommunicationActivity", "Start Class Button Pressed")
        }

        endClassbtn = findViewById(R.id.endClassBtn)

        endClassbtn.setOnClickListener{view : View ->
            closeGroup(view)
        }

        classInfoText = findViewById(R.id.ClassInformation)
        classPasswordtext = findViewById(R.id.classPassword)
        activeStudentTextView = findViewById(R.id.activeStudent)


        studentAdapter = StudentAdapter(emptyList(), object : ConnectionListener {
            override fun onStudentSelected(studentId: String) {
                handleStudentSelection(studentId)
            }
        })

        messageInput = findViewById(R.id.etMessage)
        sendButton = findViewById(R.id.send_button)

        val rvStudentList: RecyclerView = findViewById(R.id.StudentList)
        rvStudentList.adapter = studentAdapter
        rvStudentList.layoutManager = LinearLayoutManager(this)

        chatAdapter = ChatAdapter(emptyList())
        val rvChatList: RecyclerView = findViewById(R.id.rvChat)
        rvChatList.adapter = chatAdapter
        rvChatList.layoutManager = LinearLayoutManager(this)



       sendButton.setOnClickListener {
           val message = messageInput.text.toString()
           val content = ContentModel(message, deviceIp)
           if(selectedStudent.isNotEmpty() && message.isNotEmpty()) {
               server?.sendMessage(selectedStudent, content)
               messageInput.text.clear()
           } else {
               Toast.makeText(this, "Select a student and enter a message.", Toast.LENGTH_SHORT).show()
           }


       }




    }

    override fun onResume() {
        super.onResume()
        wfdManager?.also {
            registerReceiver(it, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        wfdManager?.also {
            unregisterReceiver(it)
        }
    }
    private fun createGroup(view: View) {
        wfdManager?.createGroup()
    }

    private fun closeGroup(view: View) {
        server?.close()

        wfdManager?.disconnect {
            Log.e("CommunicationActivity", "Class has been ended and group closed")
        }
        updateUI()
    }



    private fun updateUI(){
        //The rules for updating the UI are as follows:
        // IF the WFD adapter is NOT enabled then
        //      Show UI that says turn on the wifi adapter
        // ELSE IF there is NO WFD connection then i need to show a view that allows the user to either
            // 1) create a group with them as the group owner OR
            // 2) discover nearby groups
        // ELSE IF there are nearby groups found, i need to show them in a list
        // ELSE IF i have a WFD connection i need to show a chat interface where i can send/receive messages
        val wfdAdapterErrorView:ConstraintLayout = findViewById(R.id.clWfdAdapterDisabled)
        wfdAdapterErrorView.visibility = if (!wfdAdapterEnabled) View.VISIBLE else View.GONE

        val wfdNoConnectionView:ConstraintLayout = findViewById(R.id.clNoWifiDirectConnection)
        wfdNoConnectionView.visibility = if (wfdAdapterEnabled && !wfdHasConnection) View.VISIBLE else View.GONE

        val wfdConnectedView = findViewById<View>(R.id.clHasConnection)
        wfdConnectedView.visibility = if(wfdHasConnection)View.VISIBLE else View.GONE

        classInfoText.text= "Class Network : ${wfdManager?.groupInfo?.networkName}"
        classPasswordtext.text="Password : ${wfdManager?.groupInfo?.passphrase}"
    }



    override fun onWiFiDirectStateChanged(isEnabled: Boolean) {
        wfdAdapterEnabled = isEnabled
        var text = "There was a state change in the WiFi Direct. Currently it is "
        text = if (isEnabled){
            "$text enabled!"
        } else {
            "$text disabled! Try turning on the WiFi adapter"
        }

        val toast = Toast.makeText(this, text, Toast.LENGTH_SHORT)
        toast.show()
        updateUI()
    }


    override fun onGroupStatusChanged(groupInfo: WifiP2pGroup?) {
        val text = if (groupInfo == null){
            "Group is not formed"
        } else {
            "Group has been formed"
        }
        val toast = Toast.makeText(this, text , Toast.LENGTH_SHORT)
        toast.show()
        wfdHasConnection = groupInfo != null

        if (groupInfo == null){
            server?.close()
        }else if (groupInfo.isGroupOwner && server == null){
                server = Server(this)
                deviceIp = "192.168.49.1"

            }
        updateUI()
    }

    override fun onDeviceStatusChanged(thisDevice: WifiP2pDevice) {
        val toast = Toast.makeText(this, "Device parameters have been updated" , Toast.LENGTH_SHORT)
        toast.show()
    }

    override fun onStudentConnected(studentAddress: String) {
        Log.d("CommunicationActivity", "Student Connected: $studentAddress")
    }

    override fun onStudentsUpdated(students: List<String>) {
        runOnUiThread { studentAdapter.updateStudents(students) }
    }

    override fun onMessage(studentId: String, messageList: List<ContentModel>) {
        runOnUiThread {
            if (studentId == selectedStudent) {
                chatAdapter.updateMessages(messageList)
            }
        }
    }


    private fun handleStudentSelection(studentId: String) {
        selectedStudent = studentId
        activeStudentTextView.text = "Student Chat - $selectedStudent"
        val messageList = server?.studentMessages?.get(studentId)
        if (messageList !=null) {
            runOnUiThread { chatAdapter.updateMessages(messageList) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wfdManager?.disconnect { Log.d("CommunicationActivity","Disconnecting on Close") }
        unregisterReceiver(wfdManager)
        server?.close()


    }




}