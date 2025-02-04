package nl.tudelft.trustchain.detoks

import AdminWallet
import Wallet
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputLayout
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_offline_transfer.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentTokenListBinding

class TokenListFragment : BaseFragment(R.layout.fragment_token_list), TokenButtonListener  {

    private val myPublicKey = getIpv8().myPeer.publicKey

    private var adminWallet: AdminWallet? = null;
    private var userWallet: Wallet? = null;

    private val binding by viewBinding(FragmentTokenListBinding::bind)

    private val adapter = ItemAdapter()

    private val items: LiveData<List<Item>> by lazy {
        liveData { emit(listOf<Item>()) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val access = requireArguments().getString("access")
        print("viewInit")
        adapter.registerRenderer(TokenAdminItemRenderer(access!!, this))

        lifecycleScope.launchWhenResumed {
            while (isActive) {
                if (userWallet != null && adminWallet != null) {
                    print("iteration X")
                    if (access == "admin") {
                        print("adminbranch")
                        print(adminWallet!!.getTokens())
                        val items = adminWallet!!.getTokens().map { token: Token -> getPreviousOwner(token, userWallet!!)}
                        adapter.updateItems(items)
                    } else if (access == "user") {
                        print("Userbranch")
                        print(userWallet!!.getTokens())
                        val items = userWallet!!.getTokens().map { token: Token -> getPreviousOwner(token, userWallet!!) }
                        print(items)
                        adapter.updateItems(items)
                    }

                    adapter.notifyDataSetChanged()
                }
                delay(1000L)
            }
        }

    }
    private fun getPreviousOwner(token : Token, wallet: Wallet): TokenItem{
        if(token.recipients.size > 1){
            return TokenItem(token, wallet.getFriend(token.recipients.get(token.recipients.size - 2).publicKey))
        } else {
            return TokenItem(token, wallet.getFriend(token.lastRecipient))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_token_list, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adminWallet = AdminWallet.getInstance(view.context, myPublicKey)
        userWallet = Wallet.getInstance(view.context, myPublicKey, getIpv8().myPeer.key as PrivateKey)

        binding.list.adapter = adapter
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
//        binding.txtOwnPublicKey.text = getTrustChainCommunity().myPeer.publicKey.keyToHash().toHex()

        items.observe(viewLifecycleOwner, Observer {
            adapter.updateItems(it)
//            binding.txtBalance.text =
//                TransactionRepository.prettyAmount(transactionRepository.getMyVerifiedBalance())
        })

        //inflate dropdown menu
        val spinnerAmounts: EditText? = (view.findViewById<TextInputLayout?>(R.id.menu)).editText
        val items = listOf("0.05 EUR", "0.5 EUR", "1 EUR", "2 EUR", "5 EUR")
        val adapter = ArrayAdapter(requireContext(), R.layout.amount_dropdown, items)
        (spinnerAmounts as? AutoCompleteTextView)?.setAdapter(adapter)


        val createAdminTokenButton = view.findViewById<Button>(R.id.buttonTokenCreate)
        createAdminTokenButton?.setOnClickListener {
            val amount : String = (spinnerAmounts as AutoCompleteTextView).text.toString()

            if (!amount.equals("Choose token value") && !amount.equals("")) {
                val indexSelection = items.indexOf(amount)
                // Create a new coin and add it to both wallets!
                createNewCoin(adminWallet!!, userWallet!!, indexSelection.toByte())
                spinnerAmounts.clearListSelection()
            } else {
                Toast.makeText(this.context, "Specify the token value!", Toast.LENGTH_LONG).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNewCoin(adminWallet: AdminWallet, userWallet: Wallet, value : Byte) {
        val myPrivateKey = getIpv8().myPeer.key as PrivateKey
        val token = Token.create(value, myPublicKey.keyToBin())
        val proof = myPrivateKey.sign(token.id + token.value + token.genesisHash + myPublicKey.keyToBin())
        token.recipients.add(RecipientPair(myPublicKey.keyToBin(), proof))

        adminWallet.addToken(token)
        userWallet.addToken(token)

        println("Wallet A balance: ${adminWallet.balance}")
        println("Wallet B balance: ${userWallet.balance}")
    }
    override fun onHistoryClick(token: Token, access: String) {
        TODO("Not yet implemented")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onVerifyClick(token: Token, access: String) {
        val verified = verify(token, access)
        if (verified) {
            reissueToken(token, access)
        } else {
            Toast.makeText(this.context, "Verification failed! A participant is not correctly signed by previous one!", Toast.LENGTH_LONG).show()
        }
    }

    fun verify(@Suppress("UNUSED_PARAMETER") token: Token, access: String): Boolean {
        if (access != "admin") {
            return false
        }
//        val passVerification = token.verifyRecipients(token.verifier)
//        if(passVerification)
        return true
//        return false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun reissueToken(@Suppress("UNUSED_PARAMETER") token: Token, access: String) {
        if (access != "admin") {
            return
        }

        val reissuedToken = token.reissue()

        adminWallet?.addToken(reissuedToken)

        userWallet?.removeToken(token)
        userWallet?.addToken(reissuedToken)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment AdminFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
            WalletFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

}
