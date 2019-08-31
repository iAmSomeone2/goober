package core

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.pow

private const val OLD_LICENSEE_LOC: String = "old_licensees.csv"
private const val CART_TYPE_LIST_LOC: String = "cart_types.csv"

@ExperimentalUnsignedTypes
/**
 * The address space mapped to the game cartridge or other programs. This section is divided up into two sub-
 * sections, logically: 16KB ROM bank 00 and 16KB ROM bank 01~NN. The latter of these can be switched out with other
 * banks on the cartridge to allow for running programs larger than the GB's address space allows.
 */
object Rom {

    private val OLD_LICENSEE_MAP = mutableMapOf<UByte, String>()
    private val CART_TYPE_MAP = mutableMapOf<UByte, String>()

    /**
     * Map for determining how many ROM banks are needed.
     */
    private val ROM_BANK_MAP = mapOf(
        Pair<UByte, Int>(0x00u, 0),
        Pair<UByte, Int>(0x01u, 4),
        Pair<UByte, Int>(0x02u, 8),
        Pair<UByte, Int>(0x03u, 16),
        Pair<UByte, Int>(0x04u, 32),
        Pair<UByte, Int>(0x05u, 64),
        Pair<UByte, Int>(0x06u, 128),
        Pair<UByte, Int>(0x07u, 256),
        Pair<UByte, Int>(0x08u, 512),
        Pair<UByte, Int>(0x52u, 72),
        Pair<UByte, Int>(0x53u, 80),
        Pair<UByte, Int>(0x54u, 96)
    )

    /**
     * Map for determining how many RAM banks are needed.
     */
    private val RAM_SIZE_MAP = mapOf(
        Pair<UByte, Int>(0x00u, 0),
        Pair<UByte, Int>(0x01u, 2),
        Pair<UByte, Int>(0x02u, 8),
        Pair<UByte, Int>(0x03u, 32),
        Pair<UByte, Int>(0x04u, 128),
        Pair<UByte, Int>(0x05u, 64)
    )

    /**
     * Map for checking if a ROM has GameBoy Color features
     */
    private val CGB_FLAG_MAP = mapOf(
        Pair<UByte, String>(0x80u, "Supports CGP and works on GB"),
        Pair<UByte, String>(0xC0u, "CGB only")
    )

    /**
     * Map for checking if a ROM has Super GameBoy features
     */
    private val SGP_FLAG_MAP = mapOf(
        Pair<UByte, String>(0x00u, "No extra SGB features."),
        Pair<UByte, String>(0x30u, "Game supports SGB functions.")
    )

    /**
     * Map for checking the destination code of a ROM
     */
    private val DEST_CODE_MAP = mapOf(
        Pair<UByte, String>(0x00u, "Japanese"),
        Pair<UByte, String>(0x01u, "Non-Japanese")
    )

    init {
        loadOldLicensees()
        loadCartTypes()
    }

    /**
     * Loads the list of old-style licensees into program memory.
     */
    private fun loadOldLicensees(){
        val oldLicenseePath = Paths.get(this.javaClass.classLoader.getResource(OLD_LICENSEE_LOC).toURI())
        val licenseeRows: List<String>
        try {
            licenseeRows = Files.readAllLines(oldLicenseePath)
        } catch (e: IOException) {
            e.printStackTrace()
            println("WARN: Couldn't load ROM licensee info.")
            return
        }

        for (row in licenseeRows) {
            val rowData = row?.split(',')
            val code: UByte = stringToUByte(rowData!!.get(0))
            OLD_LICENSEE_MAP[code] = rowData[1]
        }
    }

    /**
     * Reads the cart types from storage into program memory.
     */
    private fun loadCartTypes(){
        val cartTypesPath = Paths.get(this.javaClass.classLoader.getResource(CART_TYPE_LIST_LOC).toURI())
        val cartRows: List<String>
        try {
            cartRows = Files.readAllLines(cartTypesPath)
        } catch (e: IOException) {
            e.printStackTrace()
            println("WARN: Couldn't load cartridge types.")
            return
        }

        for (row in cartRows) {
            val rowData = row?.split(',')
            val code: UByte = stringToUByte(rowData!!.get(0))
            CART_TYPE_MAP[code] = rowData[1]
        }
    }

    /**
     * Takes a 2-character string and converts its values into a UByte
     * @param num the hex number as a string to convert to a UByte
     * @return UByte representation of the hexadecimal string.
     */
    private fun stringToUByte(num: String): UByte {
        var accumulator = 0
        var count = 0
        for (i in num.length-1 downTo 0) {
            val data = when (num[i]) {
                '0','1','2','3','4','5','6','7','8','9' -> {
                    num[i].toString().toInt()
                }
                'a','A' -> 10
                'b','B' -> 11
                'c','C' -> 12
                'd','D' -> 13
                'e','E' -> 14
                'f','F' -> 15
                else -> error("Invalid hex character.")
            }
            accumulator += (data * 16.toFloat().pow(count)).toInt()
            count++
        }

        return accumulator.toUByte()
    }

    private var bank00 = UByteArray(16_384)
    private var bankNN = UByteArray(16_384)

    private val bankList = mutableListOf<UByteArray>()

    private val romInfoMap = mutableMapOf<String, Any?>()

    /**
     * Loads the specified file into all of the ROM banks.
     * Loading is based on file size and not value at 0x0148. This value is, however, used as a sanity check to
     * confirm that the correct number of banks were allocated and loaded.
     * @param romPath binary file to read the ROM data from
     */
    fun loadRom(romPath: Path) {
        val fullRom: ByteArray = Files.readAllBytes(romPath)
        // The values is fullRom will get converted to UBytes when they're put into banks.
        val bankNum: Int = (fullRom.size / 16_384)   // 16KB
        val expectedBanksHex: UByte = fullRom[0x0148].toUByte()
        val expectedBanks: Int = (ROM_BANK_MAP[expectedBanksHex] ?: error("ROM size not mapped.")).toInt()

        if (bankNum != expectedBanks && bankNum != 2) {
            println("WARN: Actual ROM size does not match expected size.")
            // I should probably do more with this later.
        }

        // Split the rom into banks and convert to UBytes
        for (i in 0 until bankNum) {
            val tempBank = UByteArray(16_384)
            for (j in 0 until 16_384) {
                val fullRomLoc = (16_384 * i) + j
                val data: UByte = fullRom[fullRomLoc].toUByte()
                tempBank[j] = data
            }

            if (i == 0) {
                bank00 = tempBank
            }
            bankList.add(tempBank)
        }
        saveRomInfo()
    }

    /**
     * Grabs relevant info out of the ROM's cartridge header and stores it for later use.
     */
    private fun saveRomInfo(){
        val title = bank00.sliceArray(0x0134..0x0143)
        romInfoMap["title"] = parseBytesAsString(title) ?: "NO TITLE"
        val mfgCode = bank00.sliceArray(0x013F..0x0142)
        romInfoMap["mfgCode"] = parseBytesAsString(mfgCode)     ?: "NO MFG CODE"
        romInfoMap["cgbFlag"] = CGB_FLAG_MAP[bank00[0x0143]]    ?: "NOT SPECIFIED"
        romInfoMap["sgbFlag"] = SGP_FLAG_MAP[bank00[0x0146]]    ?: "NOT SPECIFIED"
        romInfoMap["cartType"] = CART_TYPE_MAP[bank00[0x0147]]  ?: "NOT SPECIFIED"
        romInfoMap["romSize"] = ROM_BANK_MAP[bank00[0x0148]]    ?: "NOT SPECIFIED"
        romInfoMap["ramSize"] = RAM_SIZE_MAP[bank00[0x0149]]    ?: "NOT SPECIFIED"
        romInfoMap["destCode"] = DEST_CODE_MAP[bank00[0x014A]]  ?: "NOT SPECIFIED"
        romInfoMap["licensee"] = getLicensee(bank00[0x014B])
        romInfoMap["verNum"] = bank00[0x014C]
    }

    /**
     * Returns the appropriate licensee based on the value passed in.
     * @param value UByte value from address 0x014B in ROM
     * @return String containing the licensee name if it was found.
     */
    private fun getLicensee(value: UByte): String {
         return if (value == 0x33u.toUByte()) {
            // Get info from bytes 0x0144 and 0x0145
            val asciiCode = bank00.sliceArray(0x0144..0x0145)
            parseBytesAsString(asciiCode) ?: "none"
        } else {
            OLD_LICENSEE_MAP[value] ?: "none"
        }
    }

    /**
     * Converts a UByteArray to a String
     * @param characters the UByteArray representing the characters
     * @return a string of the converted characters. 'null' is returned if there were no characters.
     */
    private fun parseBytesAsString(characters: UByteArray): String? {
        var convString = ""
        for (data in characters) {
            val letter: Char
            if (data != 0u.toUByte()){
                letter = data.toByte().toChar()
                convString += letter
            } else {
                break
            }
        }

        return if (convString == "") {
            null
        } else {
            convString
        }
    }

    /**
     * Returns the relevant info from the romInfoMap if the key exists.
     */
    fun getInfo(infoKey: String): Any? {
        return if (romInfoMap.containsKey(infoKey)) {
            romInfoMap[infoKey]
        } else {
            null
        }
    }

    fun read(addr: UShort): UByte {
        return if (addr <= 0x3FFFu) {
            // Read from bank00
            bank00[addr.toInt()]
        } else {
            val location = (addr - 0x4000u).toInt()
            bankNN[location]
        }
    }

    /**
     * Sets which memory bank to use in place of bankNN
     * @param bankNum the bank to switch bankNN over to.
     */
    fun setBank(bankNum: Int) {
        if (bankNum in 1 until bankList.size) {
            bankNN = bankList[bankNum]
        }
    }
}