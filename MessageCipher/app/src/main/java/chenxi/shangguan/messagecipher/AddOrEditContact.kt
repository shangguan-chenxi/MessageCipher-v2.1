package chenxi.shangguan.messagecipher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import chenxi.shangguan.messagecipher.databinding.ActivityAddOrEditContactBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class AddOrEditContact : AppCompatActivity() {

    private lateinit var ui : ActivityAddOrEditContactBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_add_or_edit_contact)
        ui = ActivityAddOrEditContactBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // 去掉最顶上的刘海
        supportActionBar?.hide()

        // 密码库
        val secLib = SecurityLib()

        /** 解密的AesMasterKey， 从验证Activity传过来的 */
        val masterAesKey = intent.getStringExtra("MasterAesKey")
        val contactName = intent.getStringExtra("Contact")
        val encryptedKey = intent.getStringExtra("CommunicationKey")
        var communicationKey = ""
        if (encryptedKey != "" && encryptedKey != null){
            communicationKey = secLib.aesDecrypt(encryptedKey, masterAesKey).toString()
        }

        //AlertDialog.Builder(this).setTitle("开发测试").setMessage("MasterAesKey:\n$masterAesKey").setPositiveButton("好", null).create().show()

        if(contactName != "" && contactName != null){
            ui.txtContactName.setText(contactName)
            ui.txtAESPwd.setText(communicationKey)
        }

        // 读入联系人数据
        val contactFile = File(filesDir, secLib.contactFileName)
        val fos = FileInputStream(contactFile)
        val byte = ByteArray(fos.available()) // 读入文件大小的byte之前要确定文件大小
        val len = fos.read(byte)
        val readContent = String(byte, 0, len)
        var sectors = readContent.split("\n").toMutableList()

        // 当前Activity加入到链表
        ExitApplication.instance!!.addActivity(this)

        var keys: Map<String, Any>?
        var v1: String?
        var v2: String?

        // 返回上一activity
        ui.btnBack.setOnClickListener {
            finish()
        }

        // 随机生成AES密码
        ui.btnGenRandAesPwd.setOnClickListener {
            // 清空数字签名和密文文本框
            ui.txtEncryptedTxt.text.clear()
            ui.txtAESPwd.setText(secLib.genAesRandom())
            ui.textViewHint.setText("成功生成随机通讯密钥")
        }

        // 复制己方公钥到剪贴板
        ui.btnCopyUrPubKey.setOnClickListener {
            // 将内容复制到剪贴板
            try {
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                // 创建普通字符型ClipData
                val mClipData = ClipData.newPlainText("Label", ui.txtUrPubKey.text)
                // 将ClipData内容放到系统剪贴板里。
                cm.setPrimaryClip(mClipData);
                //AlertDialog.Builder(this).setTitle("提示").setMessage("公钥已复制到剪贴板").setPositiveButton("好" , null ).create().show()
                ui.textViewHint.setText("己方RSA公钥已复制到剪贴板")
            } catch (e: Exception) {

            }
        }

        // 初始化PSA密钥对
        ui.btnGenKeys.setOnClickListener {
            // 生成rsa密钥对
            keys = secLib.initKey()
            v1 = secLib.getPublicKey(keys as Map<String, Any>)
            v2 = secLib.getPrivateKey(keys as Map<String, Any>)

            // 清空密文文本框
            ui.txtEncryptedTxt.text.clear()

            // 显示到文本框
            ui.txtUrPubKey.setText(v1)
            ui.txtUrPriKey.setText(v2)
            ui.textViewHint.setText("成功生成己方RSA密钥对")
        }

        // 从剪贴板粘贴公钥
        ui.btnPasteOppPubKey.setOnClickListener {
            // 从剪贴板获取内容
            try{
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                //获取文本
                val clipData: ClipData? = cm.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text
                    val pasteString = text.toString()

                    ui.txtOppPubKey.text.clear()
                    ui.txtOppPubKey.setText(pasteString)
                    ui.textViewHint.setText("成功复制公钥到'对方RSA密钥'文本域")
                }
            }catch (e: Exception){

            }
        }

        // 生成通讯密钥密文
        ui.btnGenCipherText.setOnClickListener {
            // 获取并判断AES pwd长度是否合规
            val communication_key = ui.txtAESPwd.text.toString()
            val ur_public_key = ui.txtUrPubKey.text.toString()
            val ur_private_key = ui.txtUrPriKey.text.toString()
            val opp_public_key = ui.txtOppPubKey.text.toString()

            val aes_pwd_crypt_communication_key = secLib.genAesRandom() // 用于加密通讯密钥的AES密钥
            var sector1 = "" // 对方公钥加密的用于加密通讯密钥的密钥
            var sector2 = "" // 己方公钥加密的用于加密通讯密钥的密钥
            var sector3 = "" // 己方私钥对通讯密钥明文的签名
            var sector4 = "" // 用密钥加密的通讯密钥
            var final_crypt_text = "" // 最终密文

            if (communication_key.length != 16 && communication_key.length != 24 && communication_key.length != 32){
                //AlertDialog.Builder(this).setTitle("错误").setMessage("密码应为16/24/32字符长度").setPositiveButton("好" , null ).create().show()
                ui.textViewHint.setText("通讯密钥应为16/24/32字符长度")
                return@setOnClickListener
            }

            if (ur_public_key == ""){
                //ui.txtEncryptedTxt.setText(secLib.encryptByPublicKey(aes_pwd, public_key))
                ui.textViewHint.setText("需要提供己方RSA公钥")
                return@setOnClickListener
            }

            if (ur_private_key == ""){
                ui.textViewHint.setText("需要提供己方RSA私钥")
                return@setOnClickListener
            }

            if (opp_public_key == ""){
                ui.textViewHint.setText("需要提供对方RSA公钥")
                return@setOnClickListener
            }

            sector1 = secLib.encryptByPublicKey(aes_pwd_crypt_communication_key, opp_public_key).toString()
            if (sector1 == ""){
                ui.textViewHint.setText("用对方RSA公钥加密保护密钥时出错")
                return@setOnClickListener
            }

            sector2 = secLib.encryptByPublicKey(aes_pwd_crypt_communication_key, ur_public_key).toString()
            if (sector2 == ""){
                ui.textViewHint.setText("用己方RSA公钥加密保护密钥时出错")
                return@setOnClickListener
            }

            sector3 = secLib.sign(aes_pwd_crypt_communication_key, ur_private_key).toString()
            if (sector3 == ""){
                ui.textViewHint.setText("用己方RSA私钥对保护密钥签名时出错")
                return@setOnClickListener
            }

            sector4 = secLib.aesEncrypt(communication_key, aes_pwd_crypt_communication_key!!).toString()
            if (sector4 == "" || sector4 == null){
                ui.textViewHint.setText("用保护密钥加密通讯密钥时出错")
                return@setOnClickListener
            }

            final_crypt_text = "${sector1}.${sector2}.${sector3}.${sector4}"

            ui.txtEncryptedTxt.text.clear()
            ui.txtEncryptedTxt.setText(final_crypt_text)

            ui.btnCopyEncryptedTxt.callOnClick()
            ui.textViewHint.setText("成功生成通讯密钥密文并已复制到剪贴板")
        }

        // 复制加密后的内容到剪贴板
        ui.btnCopyEncryptedTxt.setOnClickListener {
            // 将内容复制到剪贴板
            try {
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                // 创建普通字符型ClipData
                val mClipData = ClipData.newPlainText("Label", ui.txtEncryptedTxt.text.toString())
                // 将ClipData内容放到系统剪贴板里。
                cm.setPrimaryClip(mClipData);
                ui.textViewHint.setText("密文已复制到剪贴板")
            } catch (e: Exception) {

            }
        }

        // 解密通讯密钥密文
        ui.btnDecCipherText.setOnClickListener {
            val encrpyed_text = ui.txtEncryptedTxt.text.toString()
            val ur_public_key = ui.txtUrPubKey.text.toString()
            val ur_private_key = ui.txtUrPriKey.text.toString()
            val opp_public_key = ui.txtOppPubKey.text.toString()

            if (encrpyed_text == "") {
                //AlertDialog.Builder(this).setTitle("错误").setMessage("需要提供密文").setPositiveButton("好" , null ).create().show()
                ui.textViewHint.setText("需要提供密文")
                return@setOnClickListener
            }

            if (ur_public_key == ""){
                //ui.txtEncryptedTxt.setText(secLib.encryptByPublicKey(aes_pwd, public_key))
                ui.textViewHint.setText("需要提供己方RSA公钥")
                return@setOnClickListener
            }

            if (ur_private_key == ""){
                ui.textViewHint.setText("需要提供己方RSA私钥")
                return@setOnClickListener
            }

            if (opp_public_key == ""){
                ui.textViewHint.setText("需要提供对方RSA公钥")
                return@setOnClickListener
            }

            val sectors = encrpyed_text.split(".")
            if (sectors.count() != 4){
                ui.textViewHint.setText("密文数据格式错误")
                return@setOnClickListener
            }

            // 如果己方私钥能解开第一段RSA密文就可以得到AES密码，也就是对方发来的消息
            var aes_pwd_crypt_communication_key  = secLib.decryptByPrivateKey(sectors[0], ur_private_key)
            var ur_message = false
            var communication_key = ""

            // 这里是己方私钥解不开第一段RSA密文，就解开第二段RSA密文得到AES密码，也就是己方向对方发送的消息
            if (aes_pwd_crypt_communication_key  == ""){
                aes_pwd_crypt_communication_key = secLib.decryptByPrivateKey(sectors[1], ur_private_key)
                ur_message = true
            }

            // 如果再解不开，收工回家，报错得了
            if (aes_pwd_crypt_communication_key  == ""){
                //AlertDialog.Builder(this).setTitle("错误").setMessage("无法使用 '己方RSA私钥' 取得AES密钥").setPositiveButton("好", null).create().show()
                ui.textViewHint.setText("无法使用己方RSA私钥取得保护密钥")
                return@setOnClickListener
            }

            // 这里肯定是已经获得了AES密码
            // AES解密数据 + 检查签名，签名有问题弹框框

            // 对方发送的，对方公钥验证签名
            if(!ur_message){
                // 无对方公钥则不验证
                if (opp_public_key == ""){
                    //AlertDialog.Builder(this).setTitle("提示").setMessage("缺少 '对方RSA公钥' 无法验证数据签名").setPositiveButton("好", null).create().show()
                    ui.textViewHint.setText("缺少对方RSA公钥无法验证数据签名")
                }else{
                    if(!secLib.verify(aes_pwd_crypt_communication_key, opp_public_key, sectors[2])){
                        //AlertDialog.Builder(this).setTitle("提示").setMessage("验证签名失败，信息可能被篡改或者 '对方RSA公钥' 不匹配").setPositiveButton("好", null).create().show()
                        ui.textViewHint.setText("验证签名失败，保护密钥可能被篡改或者由于对方RSA公钥不匹配")
                    }
                }
                // 己方发送的，不需要验证签名
            }else{

            }
            communication_key = secLib.aesDecrypt(sectors[3], aes_pwd_crypt_communication_key!!).toString()
            ui.txtAESPwd.text.clear()
            ui.txtAESPwd.setText(communication_key)
            ui.textViewHint.setText("已经获得通讯密钥")
        }

        // 从剪贴板粘贴加密后的内容
        ui.btnPasteEncryptedTxt.setOnClickListener {
            // 从剪贴板获取内容
            try{
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                //获取文本
                val clipData: ClipData? = cm.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text
                    val pasteString = text.toString()

                    ui.txtEncryptedTxt.text.clear()
                    ui.txtEncryptedTxt.setText(pasteString)
                    ui.textViewHint.setText("密文已复制到文本域")
                }
            }catch (e: Exception){

            }
        }

        // 清空密文文本框
        ui.btnClearEncryptedTxt.setOnClickListener {
            ui.txtEncryptedTxt.text.clear()
            ui.textViewHint.setText("已清空密文")
        }

        // 保存联系人
        ui.btnSaveContact.setOnClickListener {
            val communication_key = ui.txtAESPwd.text.toString()
            val newContactName = ui.txtContactName.text.toString()

            if (communication_key.length != 16 && communication_key.length != 24 && communication_key.length != 32){
                //AlertDialog.Builder(this).setTitle("错误").setMessage("密码应为16/24/32字符长度").setPositiveButton("好" , null ).create().show()
                ui.textViewHint.setText("通讯密钥应为16/24/32字符长度")
                return@setOnClickListener
            }

            if(newContactName == ""){
                //AlertDialog.Builder(this).setTitle("错误").setMessage("需要填写联系人姓名").setPositiveButton("好" , null ).create().show()
                ui.textViewHint.setText("联系人姓名不能为空")
                return@setOnClickListener
            }

            val keys = secLib.aesEncrypt(communication_key, masterAesKey)
            val str = "${newContactName}.${keys}"

            // 检查新的联系人名字存不存在
            // 旧名字为空新名字重复，提示重名需要检查（新建）
            // 旧名字不为空新名字重复，提示重名需要检查（编辑）
            var index = 0
            var isExist = false
            //Log.e("开发测试", "新建编辑联系人sectors.indices：${sectors.indices.toString()}")
            for(i in sectors.indices){
                if(sectors[i] != "") { // -1: 最后结尾的\n下一行不算进去
                    var s = sectors[i].split(".")
                    if (s[0] == contactName || s[0] == newContactName){
                        //Log.e("开发测试", "输入的联系人${newContactName} ：传入的联系人${contactName}：当前联系人s[0] ${s[0]}")
                        isExist = true
                        index = i
                        break
                    }
                }
            }

            // 编辑现存
            if (isExist && contactName != null){
                //Log.e("开发测试", "输入的联系人${contactName} ：存在，编辑")
                sectors[index] = str // 修改

                var newStr = ""
                for(i in sectors.indices){
                    if(sectors[i] != "") { // -1: 最后结尾的\n下一行不算进去
                        newStr += sectors[i] + "\n"
                    }
                }
                val fos = FileOutputStream(contactFile)
                fos.write(newStr.toByteArray())
                //fos.write("\n".toByteArray()) //写入换行
                fos.close()

                AlertDialog.Builder(this).setTitle("提示").setMessage("联系人编辑成功").setPositiveButton("好", DialogInterface.OnClickListener { dialogInterface, i ->
                    finish()
                }).setOnDismissListener{
                    finish()
                }.create().show()
            }else if(isExist && contactName == null) {
                //AlertDialog.Builder(this).setTitle("提示").setMessage("重复的联系人名称，请检查").setPositiveButton("好", null).setOnDismissListener{}.create().show()
                ui.textViewHint.setText("重复的联系人名称，请检查")
            }else{
                // 添加新人
                //Log.e("开发测试", "输入的联系人${newContactName} ：不存在，添加新人")
                contactFile.appendBytes(str.toByteArray())
                contactFile.appendBytes("\n".toByteArray())

                AlertDialog.Builder(this).setTitle("提示").setMessage("联系人添加成功").setPositiveButton("好", DialogInterface.OnClickListener { dialogInterface, i ->
                    finish()
                }).setOnDismissListener{
                    finish()
                }.create().show()
            }

        }

        // 清空所有文本框和剪贴板
        ui.btnPurgeAll.setOnClickListener {
            // 清空所有文本框
            ui.txtAESPwd.text.clear()
            ui.txtUrPubKey.text.clear()
            ui.txtUrPriKey.text.clear()
            ui.txtOppPubKey.text.clear()
            ui.txtEncryptedTxt.text.clear()

            // 清空密钥
            keys = HashMap(0)
            v1 = ""
            v2 = ""

            //清空剪贴板
            try{
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val mClipData = ClipData.newPlainText("", "")
                cm.setPrimaryClip(mClipData)
            }catch (e: Exception){

            }
            //AlertDialog.Builder(this).setTitle("提示").setMessage("重置成功").setPositiveButton("好" , null ).create().show()
            ui.textViewHint.setText("已清空所有文本域")
        }

        // 退出程序
        ui.btnExitProgram.setOnClickListener {
            ExitApplication.instance?.exit()
        }


/*
        // 私钥加密 AES pwd
        ui.btnPrivateKeyEncrypt.setOnClickListener {
            var aes_pwd = ui.txtAESPwd.text.toString()
            var private_key = ui.txtPrivateKey.text.toString()

            if (aes_pwd.length != 16 && aes_pwd.length != 24 && aes_pwd.length != 32){
                AlertDialog.Builder(this).setTitle("错误").setMessage("密码应为16/24/32字符长度").setPositiveButton("好" , null ).create().show()
            }else{
                if (private_key != ""){
                    ui.txtEncryptedTxt.setText(secLib.encryptByPublicKey(aes_pwd, private_key))
                }else{
                    AlertDialog.Builder(this).setTitle("错误").setMessage("需要提供私钥").setPositiveButton("好" , null ).create().show()
                }
            }
        }

        // 私钥解密 encrpyed text
        ui.btnPrivateKeyDecrypt.setOnClickListener {
            // 获取已加密的文本和私钥
            var encrypted_text = ui.txtEncryptedTxt.text.toString()
            var private_key = ui.txtPrivateKey.text.toString()

            if(encrypted_text == ""){
                AlertDialog.Builder(this).setTitle("错误").setMessage("需要提供密文").setPositiveButton("好" , null ).create().show()
            }else if (private_key == ""){
                AlertDialog.Builder(this).setTitle("错误").setMessage("需要提供私钥").setPositiveButton("好" , null ).create().show()
            }else{
                ui.txtAESPwd.setText(secLib.decryptByPrivateKey(encrypted_text, private_key))
            }
        }

        // 私钥对加密后的数据签名
        ui.btnGenSignature.setOnClickListener {
            var encrpyed_text = ui.txtEncryptedTxt.text.toString()
            var private_key = ui.txtPrivateKey.text.toString()
            var aes_pwd = ui.txtAESPwd.text.toString()

            if (private_key != "") {
                if (encrpyed_text == "") {
                    if (aes_pwd.length != 16 && aes_pwd.length != 24 && aes_pwd.length != 32) {
                        AlertDialog.Builder(this).setTitle("错误")
                            .setMessage("密码应为16/24/32字符长度").setPositiveButton("好" , null ).create().show()
                    } else {
                        ui.txtEncryptedTxt.setText(secLib.encryptByPrivateKey(aes_pwd, private_key))
                        ui.txtSignature.setText(
                            secLib.sign(
                                ui.txtEncryptedTxt.text.toString(),
                                private_key
                            )
                        )

                        // 将内容复制到剪贴板
                        try {
                            //获取剪贴板管理器
                            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            // 创建普通字符型ClipData
                            val mClipData = ClipData.newPlainText("Label", ui.txtSignature.text)
                            // 将ClipData内容放到系统剪贴板里。
                            cm.setPrimaryClip(mClipData);
                            AlertDialog.Builder(this).setTitle("提示").setMessage("已完成加密和签名并已复制到剪贴板").setPositiveButton("好" , null ).create().show()
                        } catch (e: Exception) {

                        }
                    }
                }else{
                    ui.txtSignature.setText(secLib.sign(ui.txtEncryptedTxt.text.toString(), private_key))
                    AlertDialog.Builder(this).setTitle("提示").setMessage("已完成签名并已复制到剪贴板").setPositiveButton("好" , null ).create().show()
                }
            }else{
                AlertDialog.Builder(this).setTitle("错误").setMessage("需要提供私钥").setPositiveButton("好" , null ).create().show()
            }
        }

        // 用公钥和加密数据验证签名
        ui.btnVerifySignature.setOnClickListener {
            var encrpyed_text = ui.txtEncryptedTxt.text.toString()
            var public_key = ui.txtPublicKey.text.toString()
            var signature_text = ui.txtSignature.text.toString()

            if (encrpyed_text == "") {
                AlertDialog.Builder(this).setTitle("错误").setMessage("需要提供密文").setPositiveButton("好" , null ).create().show()
            }else if (public_key == ""){
                AlertDialog.Builder(this).setTitle("错误").setMessage("需要提供公钥").setPositiveButton("好" , null ).create().show()
            }else if (signature_text == ""){
                AlertDialog.Builder(this).setTitle("错误").setMessage("需要提供数字签名").setPositiveButton("好" , null ).create().show()
            }else{
                if (secLib.verify(encrpyed_text, public_key, signature_text)){
                    ui.txtAESPwd.setText(secLib.decryptByPublicKey(encrpyed_text, public_key))
                    AlertDialog.Builder(this).setTitle("提示").setMessage("合法签名，验证成功").setPositiveButton("好" , null ).create().show()
                }else{
                    AlertDialog.Builder(this).setTitle("提示").setMessage("签名不合法，验证失败").setPositiveButton("好" , null ).create().show()
                }
            }
        }

        // 清空AES密码文本框
        ui.btnClearAesPwdTxt.setOnClickListener {
            ui.txtAESPwd.text.clear()
        }

        // 清空公钥文本框
        ui.btnClearPublicKeyTxt.setOnClickListener{
            ui.txtPublicKey.text.clear()
        }

        // 清空私钥文本框
        ui.btnClearPrivateKeyTxt.setOnClickListener {
            ui.txtPrivateKey.text.clear()
        }

        // 清空数字签名文本框
        ui.btnClearSignatureTxt.setOnClickListener {
            ui.txtSignature.text.clear()
        }

        // 从剪贴板粘贴公钥
        ui.btnPastePublicKey.setOnClickListener {
            // 从剪贴板获取内容
            try{
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                //获取文本
                val clipData: ClipData? = cm.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text
                    val pasteString = text.toString()

                    ui.txtPublicKey.text.clear()
                    ui.txtPublicKey.setText(pasteString)
                }
            }catch (e: Exception){

            }
        }

        // 复制私钥到剪贴板
        ui.btnCopyPrivateKey.setOnClickListener {
            // 将内容复制到剪贴板
            try {
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                // 创建普通字符型ClipData
                val mClipData = ClipData.newPlainText("Label", ui.txtPrivateKey.text)
                // 将ClipData内容放到系统剪贴板里。
                cm.setPrimaryClip(mClipData);
                AlertDialog.Builder(this).setTitle("提示").setMessage("私钥已复制到剪贴板").setPositiveButton("好" , null ).create().show()
            } catch (e: Exception) {

            }
        }

*/



    }

}