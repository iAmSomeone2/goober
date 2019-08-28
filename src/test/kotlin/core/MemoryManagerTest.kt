package core

import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.LocalDateTime

private const val TEST_FILE_NAME: String = "randomBytes"

@ExperimentalUnsignedTypes
class MemoryManagerTest {

    @Test fun `write to and read from vRAM`() {
        val mmu = MemoryManager
        val random = Random(LocalDateTime.now().second)
        val testVal: UByte = random.nextInt(0xFF).toUByte()
        val testAddr: UShort = random.nextInt(0x8000, 0x9FFF).toUShort()

        mmu.writeByte(testAddr, testVal)
        assertEquals(testVal, mmu.readByte(testAddr))
    }

    @Test fun `write to and read from exRAM`() {
        val mmu = MemoryManager
        val random = Random(LocalDateTime.now().second)
        val testVal: UByte = random.nextInt(0xFF).toUByte()
        val testAddr: UShort = random.nextInt(0xA000, 0xBFFF).toUShort()

        mmu.writeByte(testAddr, testVal)
        assertEquals(testVal, mmu.readByte(testAddr))
    }

    @Test fun `write to and read from work RAM without banking`() {
        val mmu = MemoryManager
        val random = Random(LocalDateTime.now().second)
        val testVal: UByte = random.nextInt(0xFF).toUByte()
        val testAddr: UShort = random.nextInt(0xC000, 0xDFFF).toUShort()

        mmu.writeByte(testAddr, testVal)
        assertEquals(testVal, mmu.readByte(testAddr))
    }

    @Test fun `write to work RAM and read from ECHO RAM (no banking)`() {
        val mmu = MemoryManager
        val random = Random(LocalDateTime.now().second)
        val testVal: UByte = random.nextInt(0xFF).toUByte()
        val testAddr: UShort = random.nextInt(0xC000, 0xDDFF).toUShort()
        val echoAddr: UShort = (testAddr + 0x2000u).toUShort()

        mmu.writeByte(testAddr, testVal)
        assertEquals(testVal, mmu.readByte(echoAddr))
    }

    @Test fun `write to ECHO RAM and read from work RAM (no banking)`() {
        val mmu = MemoryManager
        val random = Random(LocalDateTime.now().second)
        val testVal: UByte = random.nextInt(0xFF).toUByte()
        val testAddr: UShort = random.nextInt(0xE000, 0xFDFF).toUShort()
        val ramAddr: UShort = (testAddr - 0x2000u).toUShort()

        mmu.writeByte(testAddr, testVal)
        assertEquals(testVal, mmu.readByte(ramAddr))
    }

    @Test fun `write to and read from OAM`() {
        val mmu = MemoryManager
        val random = Random(LocalDateTime.now().second)
        val testVal: UByte = random.nextInt(0xFF).toUByte()
        val testAddr: UShort = random.nextInt(0xFE00, 0xFE9F).toUShort()

        mmu.writeByte(testAddr, testVal)
        assertEquals(testVal, mmu.readByte(testAddr))
    }

    @Test fun `safe usage of unusable addresses`() {
        val mmu = MemoryManager
        val random = Random(LocalDateTime.now().second)
        val testVal: UByte = random.nextInt(0xFF).toUByte()
        val testAddr: UShort = random.nextInt(0xFEA0, 0xFEFF).toUShort()

        mmu.writeByte(testAddr, testVal)
        assertEquals(0x00u.toUByte(), mmu.readByte(testAddr))
    }

    @Test fun `write to and read from IO registers`(){
        val mmu = MemoryManager
        val random = Random(LocalDateTime.now().second)
        val testVal: UByte = random.nextInt(0xFF).toUByte()
        val testAddr: UShort = random.nextInt(0xFF00, 0xFF7F).toUShort()

        mmu.writeByte(testAddr, testVal)
        assertEquals(testVal, mmu.readByte(testAddr))
    }

    @Test fun `write to and read from high RAM`() {
        val mmu = MemoryManager
        val random = Random(LocalDateTime.now().second)
        val testVal: UByte = random.nextInt(0xFF).toUByte()
        val testAddr: UShort = random.nextInt(0xFF80, 0xFFFE).toUShort()

        mmu.writeByte(testAddr, testVal)
        assertEquals(testVal, mmu.readByte(testAddr))
    }

    @Test fun `write to and read from Interrupts Enable Register`() {
        val mmu = MemoryManager
        val random = Random(LocalDateTime.now().second)
        val testVal: UByte = random.nextInt(0xFF).toUByte()
        val testAddr: UShort = 0xFFFFu

        mmu.writeByte(testAddr, testVal)
        assertEquals(testVal, mmu.readByte(testAddr))
    }

    @Test fun `load random binary data as ROM file`() {
        val bankNum = 512 // Max size
        val testFile = makeTestFile(bankNum)

        val mmu = MemoryManager
        mmu.loadRomFromFile(testFile.toString())
    }

    /**
     * Takes care of creating a test file full of binary data
     */
    private fun makeTestFile(bankNum: Int): Path{
        val tempDir = Files.createTempDirectory("")
        val tempFile = Files.createTempFile(tempDir, TEST_FILE_NAME, ".gb")
        val outStream = DataOutputStream(FileOutputStream(tempFile.toFile()))

        val fullSize = bankNum * 16_384

        val random = Random(LocalDateTime.now().second)

        for (i in 0 until fullSize) {
            val writeByte: UByte

            if ( i == 0x0148) {
                // We need to set this specific byte in order to pass the file size test
                writeByte = when (bankNum) {
                    2 -> 0x00u
                    4 -> 0x01u
                    8 -> 0x02u
                    16 -> 0x03u
                    32 -> 0x04u
                    64 -> 0x05u
                    128 -> 0x06u
                    256 -> 0x07u
                    512 -> 0x08u
                    72 -> 0x52u
                    80 -> 0x53u
                    96 -> 0x54u
                    else -> error("Invalid number of banks.")
                }
            } else {
                writeByte = random.nextInt(0xFF).toUByte()
            }

            outStream.writeByte(writeByte.toInt())
        }

        outStream.close()
        return tempFile
    }
}