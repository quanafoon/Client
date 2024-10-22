package dev.kwasi.echoservercomplete.peerlist

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pDevice
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.kwasi.echoservercomplete.R

class PeerListAdapter(private val iFaceImpl:PeerListAdapterInterface, private val idEditText: EditText): RecyclerView.Adapter<PeerListAdapter.ViewHolder>() {
    private val peersList:MutableList<WifiP2pDevice> = mutableListOf()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.peer_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val peer = peersList[position]

        holder.titleTextView.text = peer.deviceName
        holder.descriptionTextView.text = peer.deviceAddress

        holder.itemView.setOnClickListener {
            val idText = idEditText.text.toString()
            val enteredValue = idText.toIntOrNull()

            if (enteredValue!= null && enteredValue in 816000000..816999999){
                Log.d("CommunicationActivity", "Valid Student id $enteredValue")
                iFaceImpl.onPeerClicked(peer)
            } else {
                idEditText.error = "Please enter a valid student id between 816000000 and 816999999"
            }
        }
    }

    override fun getItemCount(): Int {
        return peersList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newPeersList:Collection<WifiP2pDevice>){
        peersList.clear()
        peersList.addAll(newPeersList)
        notifyDataSetChanged()
    }
}