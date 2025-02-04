package nl.tudelft.trustchain.detoks

import Wallet
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.fragment_offline_transfer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.detoks.db.DbHelper
import java.io.ByteArrayOutputStream
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.collections.ArrayList

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [OfflineTransferFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OfflineTransferFragment : BaseFragment(R.layout.fragment_offline_transfer) {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    var balanceText: TextView? = null
    var totalBalanceText: TextView? = null
    var balanceText2: TextView? = null
    var balanceText1: TextView? = null
    var balanceText05: TextView? = null
    var balanceText005: TextView? = null
    var arrayAdapter: ArrayAdapter<String>? = null
    var wallet : Wallet? = null
    var spinnerFriends: EditText? = null
    var spinnerOuter: TextInputLayout? = null

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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_offline_transfer, container, false)
        wallet = Wallet.getInstance(view.context, getIpv8().myPeer.publicKey, getIpv8().myPeer.key as PrivateKey)

        val friends = wallet!!.getListOfFriends()
        val friendUsernames = mutableListOf<String>()
        for (f in friends){
            friendUsernames.add(f.username)
        }

        spinnerOuter = view.findViewById(R.id.spinner)
        spinnerFriends = (view.findViewById<TextInputLayout?>(R.id.spinner)).editText
        // create an array adapter and pass the required parameter
        // in our case pass the context, drop down layout, and array.
        arrayAdapter = ArrayAdapter(view.context, R.layout.amount_dropdown, friendUsernames)
        (spinnerFriends as? AutoCompleteTextView)?.setAdapter(arrayAdapter)
        // set adapter to the spinner
//        spinnerFriends?.adapter = arrayAdapter
//        arrayAdapter?.notifyDataSetChanged()
//        spinnerFriends?.refreshDrawableState()

        // Inflate the layout for this fragment
        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val myPublicKey = getIpv8().myPeer.publicKey
        val myPrivateKey = getIpv8().myPeer.key as PrivateKey
        val amountText = view.findViewById<EditText>(R.id.amount)

        updateBalance(view)

        val buttonScan = view.findViewById<Button>(R.id.button_send)
        buttonScan.setOnClickListener {
            qrCodeUtils.startQRScanner(this, null, true)
        }

        val buttonAddFriend = view.findViewById<Button>(R.id.button_friend)
        buttonAddFriend.setOnClickListener {
            val navController = view.findNavController()
            navController.navigate(R.id.addFriendFragment)
        }


        val token = Token.create(1, myPublicKey.keyToBin())
        val proof = myPrivateKey.sign(token.id + token.value + token.genesisHash + myPublicKey.keyToBin())
        token.recipients.add(RecipientPair(myPublicKey.keyToBin(), proof))


        Toast.makeText(this.context, "Balance " + wallet!!.balance.toString(), Toast.LENGTH_LONG).show()

        val buttonRequest = view.findViewById<Button>(R.id.button_request)
        val transferAmountDetails = view.findViewById<TextView>(R.id.transferDetails)
        val dbHelper = DbHelper(view.context)
        buttonRequest.setOnClickListener {
            //friend selected
            val friendUsername = (spinnerFriends as AutoCompleteTextView).text.toString()
            val amount = amountText.text

//            amountText.clearFocus()
            //get the friends public key from the db
            val friendPublicKey = dbHelper.getFriendsPublicKey(friendUsername)
            var details = "Scan the QR Code to Receive Money!!\nSending $amount Euros to $friendUsername."

            try {
                //if(amount.toString().toInt() >= 0) {
                        if (wallet!!.balance > 0 && friendUsername != "") {

                            spinnerOuter?.visibility = View.INVISIBLE
                            amountText?.visibility = View.INVISIBLE
                            buttonRequest.visibility = View.INVISIBLE

                            val chosenTokens = wallet!!.getPayment(amount.toString().toDouble(),
                             0.0, ArrayList<Token>())
                            if(chosenTokens == null){
                                Toast.makeText(this.context, "Not Successful (not enough money or could get amount)", Toast.LENGTH_LONG).show()
                            } else {
                                transferAmountDetails.text = details
                                transferAmountDetails.textAlignment = View.TEXT_ALIGNMENT_CENTER
                                showQR(view, chosenTokens, friendPublicKey)
                                Toast.makeText(this.context, "Successful " + wallet!!.balance.toString(), Toast.LENGTH_LONG).show()
                                updateBalance(view)
//                                this.balanceText?.text = wallet!!.balance.toString()
                            }
                        } else {
                            if(friendUsername == ""){
                                Toast.makeText(this.context, "Specify receiver!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this.context, "No money - balance is 0", Toast.LENGTH_LONG).show()
                            }
                        }
                //TODO: disappear text field, button, spinner
                // write some message you are sending blaabla
                // check amount more than 0
            } catch (e : NumberFormatException){
                Toast.makeText(this.context, "Please specify positive amount!", Toast.LENGTH_LONG).show()
            }

        }

//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment OfflineTransferFragment.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            OfflineTransferFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
    }

    private fun updateBalance(view: View) {
        totalBalanceText = view.findViewById(R.id.totalBalance)
        balanceText = view.findViewById(R.id.txtBalance)
        balanceText2 = view.findViewById(R.id.txtBalance2)
        balanceText1 = view.findViewById(R.id.txtBalance1)
        balanceText05 = view.findViewById(R.id.txtBalance05)
        balanceText005 = view.findViewById(R.id.txtBalance005)

        var balance = "Total Balance: " + wallet!!.balance.toString()
        this.totalBalanceText?.text = balance

        balance = "5 EUR: " + wallet!!.getTokensPerValue(5.0).toString()
        this.balanceText?.text = balance

        balance = "2 EUR: " + wallet!!.getTokensPerValue(2.0).toString()
        this.balanceText2?.text = balance

        balance = "1 EUR: " + wallet!!.getTokensPerValue(1.0).toString()
        this.balanceText1?.text = balance

        balance = "0.5 EUR: " + wallet!!.getTokensPerValue(0.5).toString()
        this.balanceText05?.text = balance

        balance = "0.05 EUR: " + wallet!!.getTokensPerValue(0.05).toString()
        this.balanceText005?.text = balance
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
        val content = qrCodeUtils.parseActivityResult(requestCode, resultCode, data)
        Log.v("Transfer data ", content.toString())


        if (content != null) {
            val proba = ungzip(content)

            val dateJsonDeserializer = object : JsonDeserializer<LocalDateTime> {
                val formatter: DateTimeFormatter =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                override fun deserialize(
                    json: JsonElement?, typeOfT: Type?,
                    context: JsonDeserializationContext?
                ): LocalDateTime {
                    return LocalDateTime.parse(
                        json?.getAsString(),
                        formatter
                    )
                }
            }
            val gsonObject =
                GsonBuilder().registerTypeAdapter(LocalDateTime::class.java, dateJsonDeserializer)
                    .create()

            var map = object : TypeToken<ArrayList<Token>>() {}.type
            val result: ArrayList<Token> = gsonObject.fromJson(proba, map)

            //TODO:check whether tokens are sent, but not sth else
            //TODO:check whether the tokens are not empty list
//            val obtainedTokens =  result //?: return  //Token.deserialize(content.toByteArray()) //

            for (t in result) {
                val tokenPublicKey = t.recipients.last().publicKey
                val pubKey = getIpv8().myPeer.publicKey.keyToBin()
                if (tokenPublicKey.contentEquals(pubKey)) {
                    Log.v("Tokennnnn", t.toString())
                    println("New tokensss: ${wallet!!.balance}")
                    println("Tokennnnn $t.toString()")
                    val successful = wallet!!.addToken(t)
                    if (successful == -1L) {
                        Toast.makeText(this.context, "Unsuccessful!", Toast.LENGTH_LONG).show()
                    } else {
//                        this.balanceText?.text = wallet!!.balance.toString()
                        updateBalance(this.requireView())
                        Toast.makeText(this.context, "Added tokens!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this.context, "This token is not for you!", Toast.LENGTH_LONG)
                        .show()
                }
            }

        } else {
            Toast.makeText(this.context, "Scanning failed!", Toast.LENGTH_LONG).show()
        }
    }

    private fun createNextOwner(tokens : ArrayList<Token>, pubKeyRecipient: ByteArray) : ArrayList<Token> {
        val senderPrivateKey = getIpv8().myPeer.key

        // create the new ownership of the token
        for(token in tokens) {
            token.signByPeer(pubKeyRecipient, senderPrivateKey as PrivateKey)
        }
        return tokens
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun showQR(view: View, token: ArrayList<Token>, friendPublicKey: ByteArray) {
        val newTokens = createNextOwner(token, friendPublicKey)
        // encode newToken

        val dateJsonSerializer = object : JsonSerializer<LocalDateTime> {
            val  formatter : DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

            override fun serialize(localDateTime: LocalDateTime?, typeOfSrc: Type?,
                context: JsonSerializationContext?): JsonElement {
                return JsonPrimitive(formatter.format(localDateTime));
            }
        }
        val gsonObject = GsonBuilder().registerTypeAdapter(LocalDateTime::class.java, dateJsonSerializer).create()

        val result = gsonObject.toJson(newTokens)
        val compressedJSONString = gzip(result)
        hideKeyboard()
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                qrCodeUtils.createQR(compressedJSONString)
            }
            val qrCodeImage = view.findViewById<ImageView>(R.id.QR)
            qrCodeImage.setImageBitmap(bitmap)
        }
        button_send.visibility = View.INVISIBLE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun gzip(content: String): String {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter().use { it.write(content) }
        val test = bos.toByteArray()
        return  String(Base64.getEncoder().encode(test))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun ungzip(content: String): String =
        GZIPInputStream(Base64.getDecoder().decode(content).inputStream()).bufferedReader().use { it.readText() }

    private fun hideKeyboard() {
        val inputManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (inputManager.isAcceptingText) {
            inputManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
        }
    }
}
