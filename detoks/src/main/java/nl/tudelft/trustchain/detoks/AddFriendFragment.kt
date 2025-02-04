package nl.tudelft.trustchain.detoks

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.detoks.db.DbHelper
import nl.tudelft.trustchain.detoks.newcoin.OfflineFriend

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AddFriendFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AddFriendFragment : BaseFragment(R.layout.fragment_add_friend) {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_friend, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pubKey = view.findViewById<TextView>(R.id.myKey)
        var detail = "Scan Public Key of the Recipient to add in your Addressbook"

        pubKey.text = detail
        val buttonScan = view.findViewById<Button>(R.id.button_scan_public_key)
        buttonScan.setOnClickListener {
            qrCodeUtils.startQRScanner(this, null, true)
        }

        val My_QR = view.findViewById<ImageView>(R.id.My_QR)


        val buttonShow = view.findViewById<Button>(R.id.button_show)
        buttonShow.setOnClickListener {
            buttonScan.visibility = View.INVISIBLE
            pubKey.text = "Scan the QR Code to add me as a friend!"
            val myPublicKey = getIpv8().myPeer.publicKey.keyToBin()
            val gsonObject = GsonBuilder().create()
            val result = gsonObject.toJson(myPublicKey)

            lifecycleScope.launch {
                var bitmap = withContext(Dispatchers.Default) {
                    // qrCodeUtils.createQR(payload.serialize().toHex())
                    qrCodeUtils.createQR(result)
                }
                My_QR.visibility = View.VISIBLE
                My_QR.setImageBitmap(bitmap)
                buttonShow.visibility = View.INVISIBLE
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val content = qrCodeUtils.parseActivityResult(requestCode,resultCode,data)
        Log.v("PublicKey content", content.toString())

        val nameFriend = view?.findViewById<EditText>(R.id.name)
        nameFriend?.visibility = View.VISIBLE

        val buttonSave = view?.findViewById<Button>(R.id.save_friend)
        buttonSave?.visibility = View.VISIBLE

        buttonSave?.setOnClickListener{
            val username = nameFriend?.text
           if(nameFriend?.text == null){
               Toast.makeText(this.context,"Enter friend's name!", Toast.LENGTH_LONG).show()
           } else {
               if(content != null) {
                   val myPublicKey = getIpv8().myPeer.publicKey
                   val wallet = Wallet.getInstance(
                       this.requireContext(),
                       myPublicKey,
                       getIpv8().myPeer.key as PrivateKey
                   )
                   var map = object :  TypeToken<ByteArray>() {}.type
                   val result: ByteArray = Gson().fromJson(content, map)
                   val newRowId = wallet.addFriend(
                       OfflineFriend(username.toString(), result)
                   )
                   if (newRowId != -1L) {
                       Toast.makeText(this.context, "Added Friend!", Toast.LENGTH_LONG).show()
                   } else {
                       Toast.makeText(this.context, "Duplicate Entry!", Toast.LENGTH_LONG).show()
                   }
                   //save call to db

                   Log.v("Save pub key", content.toString())
                   Log.v("Name ", username.toString())
                   val navController = this.findNavController()
                   navController.navigate(R.id.offlineTransferFragment)
               } else {
                   Toast.makeText(this.context, "Scanning failed!", Toast.LENGTH_LONG).show()
               }
           }
        }

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AddFriendFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AddFriendFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
