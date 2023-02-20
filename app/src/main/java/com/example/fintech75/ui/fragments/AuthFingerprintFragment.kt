package com.example.fintech75.ui.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.fintech75.R
import com.example.fintech75.application.AppConstants
import com.example.fintech75.core.Resource
import com.example.fintech75.data.model.FingerprintSample
import com.example.fintech75.data.model.UserCredential
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.FragmentAuthFingerprintBinding
import com.example.fintech75.hardware.BtClass
import com.example.fintech75.hardware.CONNECTING_STATUS
import com.example.fintech75.hardware.MESSAGE_READ
import com.example.fintech75.hardware.MESSAGE_WRITE
import com.example.fintech75.presentation.*
import com.example.fintech75.repository.MovementRepositoryImpl
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity
import retrofit2.HttpException
import java.security.PrivateKey
import java.util.*

class AuthFingerprintFragment : Fragment(R.layout.fragment_auth_fingerprint) {
    private val _BLUETOOTH_PERMISSION = 1
    private val streamCompleteRegex = """(?i).*stream complete.*""".toRegex()

    private val fragmentName = this::class.java.toString()
    private val args: AuthFingerprintFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels {
        UserViewModelFactory(
            StartRepositoryImpl(
                RemoteDataSource(RetrofitClient.webService)
            )
        )
    }
    private val movementViewModel: MovementViewModel by viewModels {
        MovementViewModelFactory(
            MovementRepositoryImpl(
                RemoteDataSource(RetrofitClient.webService)
            )
        )
    }
    private val fingerprintViewModel: FingerprintViewModel by viewModels()

    private lateinit var binding: FragmentAuthFingerprintBinding
    private lateinit var screenLoading: RelativeLayout

    private lateinit var btClass: BtClass
    private lateinit var mHandler: Handler

    private var mFingerprintBuffer: IntArray = IntArray(36864)

    private lateinit var currentUser: UserCredential
    private var userPrivateKey: PrivateKey? = null

    private var bluetoothAddress: String? = null
    private var bluetoothName: String? = null
    private var bluetoothUuid: UUID? = null

    private var timerFinished: Boolean = false
    private var capturedFinished: Boolean = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mHandler = handler
        btClass = BtClass(mHandler)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAuthFingerprintBinding.bind(view)
        screenLoading = binding.rlAuthFingerprintLoading

        setup()
    }

    private fun setup() {
        binding.tvAuthFingerprintMsg.text = getString(R.string.loading)
        screenLoading.visibility = View.VISIBLE
        setStateMovementButtons(false, false)

        currentUserListener()
        userPrivateKeyListener()

        catchResultFromDialogs()
        backPressedListener()

        if (currentUser.userID == -1) {
            showInvalidCredentialsDialog()
        } else {
            bind()

            configConnectButton()
            configCancelButton()
            configCaptureButton()

            setStateMovementButtons(true, false)
            screenLoading.visibility = View.GONE
        }
    }

    // Credentials
    private fun currentUserListener() {
        currentUser = userViewModel.getCurrentUser().value ?: UserCredential(
            token = "N/A",
            userID = -1,
            typeUser = "N/A",
            idType = "N/A",
            email = "N/A"
        )
    }

    private fun userPrivateKeyListener() {
        userPrivateKey = userViewModel.getUserPrivateKey().value
    }
    // End Credentials

    // UI
    private fun bind() {
        if (args.idMovement == -1) {
            showInvalidCredentialsDialog()
        }

        binding.tvTypeMovement.text = args.typeMovement
        binding.tvMethodMovement.text = args.methodMovement
        binding.tvAmountMovement.text = getAmountStringFromFloat(args.amountMovement)
    }

    private fun getAmountStringFromFloat(amountFloat: Float): String {
        var newAmountString = amountFloat.toString()

        if (newAmountString.contains("""\.\d{1,2}""".toRegex())) {
            if (newAmountString.matches("""^\d+\.\d$""".toRegex())) {
                newAmountString += "0"
            }
        } else {
            newAmountString += ".00"
        }
        newAmountString = "$$newAmountString"

        return newAmountString
    }

    private fun setStateMovementButtons(cancelIsAvailable: Boolean, captureIsAvailable: Boolean) {
        binding.bCancelMovement.isEnabled = cancelIsAvailable
        binding.bCapture.isEnabled = captureIsAvailable

        binding.llButtons.visibility = if (!(cancelIsAvailable || captureIsAvailable)) {
            View.GONE
        } else {
            View.VISIBLE
        }

        binding.bCancelMovement.background = if (cancelIsAvailable) {
            ResourcesCompat.getDrawable(resources, R.drawable.shape_close_button, context?.theme)
        } else {
            ResourcesCompat.getDrawable(resources, R.drawable.shape_disable_button, context?.theme)
        }

        binding.bCapture.background = if (captureIsAvailable) {
            ResourcesCompat.getDrawable(resources, R.drawable.shape_ok_button, context?.theme)
        } else {
            ResourcesCompat.getDrawable(resources, R.drawable.shape_disable_button, context?.theme)
        }
    }

    private fun configCancelButton() {
        binding.bCancelMovement.setOnClickListener {
            cancelMovement()
        }
    }

    private fun configConnectButton() {
        binding.bConnect.isEnabled = true

        binding.bConnect.setOnClickListener {
            if (btClass.mState == BtClass.STATE_NONE) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    BtClass.bluetoothManager?.let { btManager ->
                        turnOnBluetooth(btManager)
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED) {
                        BtClass.bluetoothManager?.let { btManager ->
                            turnOnBluetooth(btManager)
                        }
                    } else {
                        requestBluetoothPermission()
                    }
                }
            } else {
                btClass.stopBluetooth()
                setStateMovementButtons(true, false)
            }
        }
    }

    private fun configCaptureButton() {
        binding.bCapture.setOnClickListener {
            binding.bCapture.isEnabled = false
            setStateMovementButtons(false, false)
            binding.tvAuthFingerprintMsg.text = getString(R.string.capturing_fingerprint)
            screenLoading.visibility = View.VISIBLE
            capturedFinished = false
            timerFinished = false
            starTimer()
            captureFingerprint()
        }
    }
    // End UI

    // Functions

    private fun routeMovement() {
        btClass.stopBluetooth()
        if (args.authType == AppConstants.AUTH_BOTH) {
            goToAuthPayPal()
        } else {
            goToResultScreen(true)
        }
    }

    private fun authFingerprint(fingerprintB64: String) {
        userPrivateKey?.let { privateKey ->
            val fsObject = FingerprintSample(fingerprintB64)
            movementViewModel.authFingerprintMovement(
                currentUser.token, args.idMovement, fsObject, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Beginning auth movement process")
                        binding.tvAuthFingerprintMsg.text = getString(R.string.verifying_fingerprint)
                        screenLoading.visibility = View.VISIBLE

                        binding.bConnect.isEnabled = false
                        setStateMovementButtons(false, false)
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Finish auth movement process")
                        routeMovement()
                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "Try auth movement again")
                        binding.bConnect.isEnabled = true
                        val capturedButtonAvailable = btClass.mState != BtClass.STATE_NONE
                        setStateMovementButtons(true, capturedButtonAvailable)
                        screenLoading.visibility = View.GONE

                        showTryAgainAuthMovementDialog()
                    }
                    is Resource.Failure -> {
                        Log.d(fragmentName, "Fail movement credit")
                        when((result.exception as HttpException).code()) {
                            401 -> {
                                Log.d(fragmentName, "Bad credentials")
                                btClass.stopBluetooth()
                                showInvalidCredentialsDialog()
                            }
                            403 -> {
                                Log.d(fragmentName, "Expired movement")
                                btClass.stopBluetooth()
                                goToResultScreen(false)
                            }
                            404 -> {
                                Log.d(fragmentName, "Client not found")
                                btClass.stopBluetooth()
                                goToResultScreen(false)
                            }
                            409 -> {
                                Log.d(fragmentName, "No enough founds")
                                btClass.stopBluetooth()
                                goToResultScreen(false)
                            }
                            450 -> {
                                Log.d(fragmentName, "Unauthorized to use this credit")
                                screenLoading.visibility = View.GONE

                                btClass.stopBluetooth()
                                goToResultScreen(false)
                            }
                            460 -> {
                                Log.d(fragmentName, "Movement conflict")
                                screenLoading.visibility = View.GONE

                                btClass.stopBluetooth()
                                goToResultScreen(false)
                            }
                            451 -> {
                                Log.d(fragmentName, "Client have already credit with the market")
                                screenLoading.visibility = View.GONE

                                btClass.stopBluetooth()
                                goToResultScreen(false)
                            }
                            480, 482 -> {
                                val actualCode = result.exception.code()
                                Log.d(fragmentName, "Could not reconstruction fingerprint: $actualCode")
                                screenLoading.visibility = View.GONE

                                binding.bConnect.isEnabled = true
                                val capturedButtonAvailable = btClass.mState != BtClass.STATE_NONE
                                setStateMovementButtons(true, capturedButtonAvailable)

                                showBadFingerprintDialog()
                            }
                            483 -> {
                                Log.d(fragmentName, "Could not match fingerprints")
                                screenLoading.visibility = View.GONE

                                binding.bConnect.isEnabled = true
                                val capturedButtonAvailable = btClass.mState != BtClass.STATE_NONE
                                setStateMovementButtons(true, capturedButtonAvailable)

                                showNoMatchFingerprintDialog()
                            }

                            else -> {
                                Log.d(fragmentName, "Generic error")
                                screenLoading.visibility = View.GONE

                                binding.bConnect.isEnabled = true
                                val capturedButtonAvailable = btClass.mState != BtClass.STATE_NONE
                                setStateMovementButtons(true, capturedButtonAvailable)
                                btClass.stopBluetooth()

                                showTryAgainAuthMovementDialog()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun authMovement(fingerprintB64: String) {
        userPrivateKey?.let { privateKey ->
            val fsObject = FingerprintSample(fingerprintB64)
            movementViewModel.performAuthMovement(
                currentUser.token, args.idMovement, fsObject, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Beginning auth movement process")
                        binding.tvAuthFingerprintMsg.text = getString(R.string.verifying_fingerprint)
                        screenLoading.visibility = View.VISIBLE

                        binding.bConnect.isEnabled = false
                        setStateMovementButtons(false, false)
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Finish auth movement process")
                        routeMovement()
                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "Try auth movement again")
                        binding.bConnect.isEnabled = true
                        val capturedButtonAvailable = btClass.mState != BtClass.STATE_NONE
                        setStateMovementButtons(true, capturedButtonAvailable)
                        screenLoading.visibility = View.GONE

                        showTryAgainAuthMovementDialog()
                    }
                    is Resource.Failure -> {
                        Log.d(fragmentName, "Fail movement credit")
                        when((result.exception as HttpException).code()) {
                            401 -> {
                                Log.d(fragmentName, "Bad credentials")
                                btClass.stopBluetooth()
                                showInvalidCredentialsDialog()
                            }
                            403 -> {
                                Log.d(fragmentName, "Expired movement")
                                btClass.stopBluetooth()
                                goToResultScreen(false)
                            }
                            404 -> {
                                Log.d(fragmentName, "Client not found")
                                btClass.stopBluetooth()
                                goToResultScreen(false)
                            }
                            409 -> {
                                Log.d(fragmentName, "No enough founds")
                                btClass.stopBluetooth()
                                goToResultScreen(false)
                            }
                            450 -> {
                                Log.d(fragmentName, "Unauthorized to use this credit")
                                screenLoading.visibility = View.GONE

                                btClass.stopBluetooth()
                                goToResultScreen(false)
                            }
                            460 -> {
                                Log.d(fragmentName, "Movement conflict")
                                screenLoading.visibility = View.GONE

                                btClass.stopBluetooth()
                                goToResultScreen(false)
                            }
                            451 -> {
                                Log.d(fragmentName, "Client have already credit with the market")
                                screenLoading.visibility = View.GONE

                                btClass.stopBluetooth()
                                goToResultScreen(false)
                            }
                            480, 482 -> {
                                val actualCode = result.exception.code()
                                Log.d(fragmentName, "Could not reconstruction fingerprint: $actualCode")
                                screenLoading.visibility = View.GONE

                                binding.bConnect.isEnabled = true
                                val capturedButtonAvailable = btClass.mState != BtClass.STATE_NONE
                                setStateMovementButtons(true, capturedButtonAvailable)

                                showBadFingerprintDialog()
                            }
                            483 -> {
                                Log.d(fragmentName, "Could not match fingerprints")
                                screenLoading.visibility = View.GONE

                                binding.bConnect.isEnabled = true
                                val capturedButtonAvailable = btClass.mState != BtClass.STATE_NONE
                                setStateMovementButtons(true, capturedButtonAvailable)

                                showNoMatchFingerprintDialog()
                            }

                            else -> {
                                Log.d(fragmentName, "Generic error")
                                screenLoading.visibility = View.GONE

                                binding.bConnect.isEnabled = true
                                val capturedButtonAvailable = btClass.mState != BtClass.STATE_NONE
                                setStateMovementButtons(true, capturedButtonAvailable)
                                btClass.stopBluetooth()

                                showTryAgainAuthMovementDialog()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun cancelMovement() {
        userPrivateKey?.let { privateKey ->
            movementViewModel.cancelMovement(
                currentUser.token, args.idMovement, privateKey
            ).observe(viewLifecycleOwner) { result: Resource<*> ->
                when(result) {
                    is Resource.Loading -> {
                        Log.d(fragmentName, "Beginning cancel credit process")
                        binding.tvAuthFingerprintMsg.text = getString(R.string.canceling)
                        screenLoading.visibility = View.VISIBLE

                        setStateMovementButtons(false, false)
                    }
                    is Resource.Success -> {
                        Log.d(fragmentName, "Finish cancel credit process")
                        btClass.stopBluetooth()

                        goToResultScreen(false)
                    }
                    is Resource.TryAgain -> {
                        Log.d(fragmentName, "Try cancel credit again")

                        val capturedButtonAvailable = btClass.mState != BtClass.STATE_NONE
                        setStateMovementButtons(true, capturedButtonAvailable)

                        screenLoading.visibility = View.GONE
                        showTryAgainCancelMovementDialog()
                    }
                    is Resource.Failure -> {
                        Log.d(fragmentName, "Fail auth credit")
                        when((result.exception as HttpException).code()) {
                            401 -> {
                                Log.d(fragmentName, "Bad credentials")
                                btClass.stopBluetooth()
                                showInvalidCredentialsDialog()
                            }
                            403 -> {
                                Log.d(fragmentName, "Expired movement")
                                btClass.stopBluetooth()
                                goToResultScreen(false)
                            }
                            404 -> {
                                Log.d(fragmentName, "Client not found")
                                btClass.stopBluetooth()
                                goToResultScreen(false)
                            }
                            450 -> {
                                Log.d(fragmentName, "Unauthorized to use this credit")
                                screenLoading.visibility = View.GONE

                                btClass.stopBluetooth()
                                goToResultScreen(false)
                            }
                            451 -> {
                                Log.d(fragmentName, "Client have already credit with the market")
                                screenLoading.visibility = View.GONE

                                btClass.stopBluetooth()
                                goToResultScreen(false)
                            }
                            480, 482 -> {
                                val actualCode = result.exception.code()
                                Log.d(fragmentName, "Could not reconstruction fingerprint: $actualCode")

                                screenLoading.visibility = View.GONE
                                setStateMovementButtons(true, true)

                                showBadFingerprintDialog()
                            }
                            483 -> {
                                Log.d(fragmentName, "Could not match fingerprint")

                                screenLoading.visibility = View.GONE
                                setStateMovementButtons(true, true)

                                showNoMatchFingerprintDialog()
                            }
                            400 -> {
                                Log.d(fragmentName, "Try cancel credit again")

                                val capturedButtonAvailable = btClass.mState != BtClass.STATE_NONE
                                setStateMovementButtons(true, capturedButtonAvailable)

                                screenLoading.visibility = View.GONE
                                showTryAgainCancelMovementDialog()
                            }

                            else -> {
                                Log.d(fragmentName, "Bad credentials")
                                screenLoading.visibility = View.GONE
                                setStateMovementButtons(false, false)

                                btClass.stopBluetooth()
                                showInvalidCredentialsDialog()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun starTimer() {
        fingerprintViewModel.activateTimer().observe(viewLifecycleOwner) { result: Boolean ->
            timerFinished = result
            if (result) {
                if (!capturedFinished) {
                    Log.d(fragmentName, "Capturing has been canceled...")
                    setStateMovementButtons(true, true)
                    binding.bCapture.isEnabled = true
                    screenLoading.visibility = View.GONE

                    Toast.makeText(
                        requireContext(),
                        "Captura cancelada. Trate de nuevo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Log.d(fragmentName, "Start timer")
            }
        }
    }

    // End Functions

    // Bluetooth

    private val getBtResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            Toast.makeText(
                requireContext(),
                R.string.bluetooth_turn_on,
                Toast.LENGTH_SHORT
            ).show()
            goToPairedDevices()
        } else {
            Toast.makeText(
                requireContext(),
                R.string.error_bluetooth_not_turn_on,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun turnOnBluetooth(bluetoothManager: BluetoothManager) {
        if (!bluetoothManager.adapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            getBtResult.launch(enableBtIntent)
        } else {
            Toast.makeText(
                requireContext(),
                R.string.bluetooth_already_on,
                Toast.LENGTH_SHORT
            ).show()
            goToPairedDevices()
        }
    }

    private fun requestBluetoothPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.BLUETOOTH_CONNECT
            )) {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Se necesita permiso")
            builder.setMessage("Es necesario el Bluetooth para autorizar movimientos mediante la huella dactilar")
            builder.setPositiveButton(R.string.i_know, DialogInterface.OnClickListener { _, _ ->
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    _BLUETOOTH_PERMISSION
                )
            })
            builder.setNegativeButton(R.string.cancel, DialogInterface.OnClickListener { dialogInterface, _ ->
                dialogInterface.dismiss()
            })
            builder.show()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                _BLUETOOTH_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == _BLUETOOTH_PERMISSION) {
            // Request for camera permission.
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Start camera preview Activity.
                goToPairedDevices()
                Toast.makeText(
                    requireContext(),
                    "Permiso obtenido",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Permission request was denied.
                Toast.makeText(
                    requireContext(),
                    "Permiso denegado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val handler: Handler = Handler(Looper.getMainLooper()) { message ->
        when (message.what) {
            CONNECTING_STATUS -> {
                when(message.arg1) {
                    1 -> {
                        // Connect with device
                        binding.bConnect.text = getString(R.string.disconnect)
                        screenLoading.visibility = View.GONE
                        setStateMovementButtons(true, true)
                        binding.bConnect.isEnabled = true
                        Toast.makeText(
                            requireContext(),
                            "$bluetoothName are connected",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                    2 -> {
                        // Disconnect with device
                        binding.bConnect.text = getString(R.string.connect_sensor)
                        setStateMovementButtons(true, false)
                        binding.bConnect.isEnabled = true
                        screenLoading.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "$bluetoothName are disconnected",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                    -1 -> {
                        // Error when try to connect
                        setStateMovementButtons(true, false)
                        screenLoading.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "Connection failed",
                            Toast.LENGTH_SHORT
                        ).show()
                        false
                    }
                    else -> {
                        // Unexpected error
                        Log.d(fragmentName, "Unexpected error")
                        false
                    }
                }
            }
            MESSAGE_READ -> {
                when (message.arg2) {
                    -1 -> {
                        // First message when connect
                        Log.d(fragmentName, "Beginning capture...")
                        binding.tvAuthFingerprintMsg.text = getString(R.string.capturing_fingerprint)
                        screenLoading.visibility = View.VISIBLE
                    }
                    1 -> {
                        // Fingerprint receive
                        mFingerprintBuffer = message.obj as IntArray

                    }
                    else -> {
                        // Finish bluetooth message
                        setStateMovementButtons(true, true)
                        binding.bCapture.isEnabled = true
                        screenLoading.visibility = View.GONE

                        if (!timerFinished) {
                            capturedFinished = true
                            Log.d(fragmentName, "Finish capture...")
                            val mBuffer: ByteArray? = message.obj as ByteArray
                            mBuffer?.let {
                                val arduinoMessage: String = String(mBuffer, 0, message.arg1)
                                if (arduinoMessage.isNotEmpty()) {
                                    if (streamCompleteRegex.containsMatchIn(arduinoMessage)) {
                                        val fingerByte = ByteArray(BtClass.SIZE_FINGERPRINT_BUFFER)

                                        // Log.d(fragmentName, "Sample size: ${mFingerprintBuffer.size}")
                                        for (i in mFingerprintBuffer.indices) {
                                            fingerByte[i] = mFingerprintBuffer[i].toByte()
                                        }

                                        val fingerprintSample: String? = Base64.getEncoder().encodeToString(fingerByte)

                                        fingerprintSample?.let { fSample ->
                                            Log.d(fragmentName, "Longitud de la muestra: ${fSample.length}")

                                            if (args.authType == AppConstants.AUTH_FINGERPRINT) {
                                                authMovement(fSample)
                                            } else {
                                                authFingerprint(fSample)
                                            }
                                        }
                                        // Log.d(fragmentName, "Sample:$fingerprintSample")
                                    }
                                }
                            }
                        }
                    }
                }
                true
            }
            MESSAGE_WRITE -> {
                when(message.arg1){
                    1 -> {
                        // Send message
                        Log.println(Log.INFO, "SENT_MESSAGE", "Message was sent successfully")
                        setStateMovementButtons(false, false)
                        binding.bConnect.isEnabled = false
                        binding.tvAuthFingerprintMsg.text = getString(R.string.capturing_fingerprint)
                        screenLoading.visibility = View.VISIBLE

                        true
                    }
                    -1 -> {
                        // Connection lost
                        Log.println(Log.INFO, "CONNECTION_LOST", "Device connection was lost")

                        val capturedButtonAvailable = btClass.mState != BtClass.STATE_NONE
                        setStateMovementButtons(true, capturedButtonAvailable)
                        binding.bConnect.isEnabled = true

                        screenLoading.visibility = View.VISIBLE
                        binding.tvAuthFingerprintMsg.text = getString(R.string.loading)

                        Toast.makeText(
                            requireContext(),
                            "Connection lost",
                            Toast.LENGTH_SHORT
                        ).show()
                        false
                    }
                    else -> {
                        // Unexpected send error
                        val capturedButtonAvailable = btClass.mState != BtClass.STATE_NONE
                        setStateMovementButtons(true, capturedButtonAvailable)
                        screenLoading.visibility = View.GONE
                        Log.println(Log.INFO, "UNEXPECTED_SEND_ERROR", "Error when we trying to send message ")
                        false
                    }
                }

            }
            else -> {
                // Error
                Log.d(fragmentName, "Unexpected Error")
                val capturedButtonAvailable = btClass.mState != BtClass.STATE_NONE
                setStateMovementButtons(true, capturedButtonAvailable)
                screenLoading.visibility = View.GONE
                false
            }
        }
    }

    private fun captureFingerprint() {
        if (btClass.mState == BtClass.STATE_CONNECTED) {
            btClass.write("S".toByteArray())
        }
    }

    // End Bluetooth

    // Navigation
    private fun goToLogin() {
        Log.d(fragmentName, "Go to Login...")
        findNavController().popBackStack(R.id.loginFragment, false)
    }

    private fun goToCredits() {
        Log.d(fragmentName, "Go to Credits...")
        findNavController().popBackStack(R.id.creditsFragment, false)
    }

    private fun goToPairedDevices() {
        Log.d(fragmentName, "Go to paired devices...")
        val action = AuthFingerprintFragmentDirections.actionAuthFingerprintFragmentToPairedDevicesFragment()
        findNavController().navigate(action)
    }

    private fun goToResultScreen(isSuccess: Boolean) {
        Log.d(fragmentName, "Go to result screen...")
        val action = AuthFingerprintFragmentDirections.actionAuthFingerprintFragmentToMovementFinishedFragment(
            isSuccessful = isSuccess
        )
        findNavController().navigate(action)
    }

    private fun goToAuthPayPal() {
        Log.d(fragmentName, "Go to Auth PayPal...")
        val action = AuthFingerprintFragmentDirections.actionAuthFingerprintFragmentToAuthPayPalFragment(
            idMovement = args.idMovement,
            typeMovement = args.typeMovement,
            amountMovement = args.amountMovement,
            methodMovement = args.methodMovement,
            authType = args.authType
        )
        findNavController().navigate(action)
    }

    private fun backPressedListener() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                Log.d(fragmentName, "Cancel auth of movement")
                cancelMovement()
            }
        })
    }

    private fun catchResultFromDialogs() {
        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Bundle>("btDevice")
            ?.observe(viewLifecycleOwner) { bundle ->
                binding.tvAuthFingerprintMsg.text = getString(R.string.setting_bluetooth)
                screenLoading.visibility = View.VISIBLE

                bluetoothAddress = bundle.getString("btAddressKey")
                bluetoothName = bundle.getString("btNameKey")
                val uuidsParcel: Array<ParcelUuid>? = bundle.get("btUUIDsKey") as Array<ParcelUuid>?

                uuidsParcel?.let {
                    val uuidString : String = it[0].toString()
                    bluetoothUuid = UUID.fromString(uuidString)
                }

                btClass.connectOutgoingDevice(bluetoothName, bluetoothAddress, bluetoothUuid)
                Log.d(fragmentName, "Device name: $bluetoothName")
            }

        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("action")
            ?.observe(viewLifecycleOwner) { action ->
                when(action) {
                    "finishSession" -> goToLogin()
                    "authMovement" -> Log.d(fragmentName, "Auth Movement")
                    "logout" -> logout()
                    "return" -> goToCredits()
                    AppConstants.ACTION_CLOSE_APP -> activity?.finish()
                    AppConstants.ACTION_CLOSE_SESSION -> logout()
                    else -> return@observe
                }
            }

        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("closed")
            ?.observe(viewLifecycleOwner) { closed ->
                when(closed) {
                    "none" -> return@observe
                    "close" -> activity?.finish()
                    "endSession" -> goToLogin()
                    "closeFragment" -> goToCredits()
                    else -> return@observe
                }
            }
    }
    // End Navigation

    // Extra Functions
    private fun logout() {
        if (currentUser.token == "N/A") {
            goToLogin()
        }
        userViewModel.logout(currentUser.token).observe(viewLifecycleOwner){ result ->
            when(result) {
                is Resource.Loading -> {
                    screenLoading.visibility = View.VISIBLE
                    binding.tvAuthFingerprintMsg.text = getString(R.string.closing_session)
                }
                is Resource.Success -> {
                    Log.d(fragmentName, "Good bye")
                    goToLogin()
                }
                is Resource.TryAgain -> {
                    screenLoading.visibility = View.GONE
                    Log.d(fragmentName, "Un error ocurrió, favor de intentarlo de nuevo")
                    showLogoutFailedDialog()
                }
                is Resource.Failure -> goToLogin()
            }
        }
    }
    // End Extra functions

    // Dialog fragments
    private fun showInvalidCredentialsDialog() = showNotificationDialog(
        title = "Credenciales invalidas",
        message = "Las credenciales han expirado",
        bOkAction = "dismiss",
        bOkText = getString(R.string.try_again),
        bOkAvailable = false,
        bCancelAction = "finishSession",
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = true,
        closeAction = "endSession"
    )

    private fun showLogoutFailedDialog() = showNotificationDialog(
        title = "Error",
        message = "No se pudo cerrar la sesión",
        bOkAction = "logout",
        bOkText = getString(R.string.try_again),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_APP,
        bCancelText = getString(R.string.close_app),
        bCancelAvailable = true
    )

    private fun showTryAgainAuthMovementDialog() = showNotificationDialog(
        title = "Movimiento no creado",
        message = "No se pudo crear el Movimiento. Intentelo de nuevo",
        bOkAction = "iKnow",
        bOkText = getString(R.string.i_know),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = false
    )

    private fun showTryAgainCancelMovementDialog() = showNotificationDialog(
        title = "Movimiento no cancelado",
        message = "No se pudo cancelar el movimiento. Intentelo de nuevo",
        bOkAction = "iKnow",
        bOkText = getString(R.string.i_know),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = false
    )

    private fun showBadFingerprintDialog() = showNotificationDialog(
        title = "Error",
        message = "La calidad de la huella es mala. Capture una nueva",
        bOkAction = "iKnow",
        bOkText = getString(R.string.i_know),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = false
    )

    private fun showNoMatchFingerprintDialog() = showNotificationDialog(
        title = "Error",
        message = "Huella no emparejada. Capture una nueva",
        bOkAction = "iKnow",
        bOkText = getString(R.string.i_know),
        bOkAvailable = true,
        bCancelAction = AppConstants.ACTION_CLOSE_SESSION,
        bCancelText = getString(R.string.close_session),
        bCancelAvailable = false
    )

    private fun showNotificationDialog(
        title: String, message: String, bOkAction: String, bOkText: String, bOkAvailable: Boolean,
        bCancelAction: String, bCancelText: String, bCancelAvailable: Boolean, closeAction: String = "none"
    ) {
        val action = AuthFingerprintFragmentDirections.actionAuthFingerprintFragmentToNotificationDialogFragment(
            title = title,
            msg = message,
            bOkAction = bOkAction,
            bOkText = bOkText,
            bOkAvailable = bOkAvailable,
            bCancelAction = bCancelAction,
            bCancelText = bCancelText,
            bCancelAvailable = bCancelAvailable,
            closeAction = closeAction
        )
        findNavController().navigate(action)
    }

}