package core

import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalUnsignedTypes
class CpuTest {
    class RegistersTest {
        private val regADefault: UByte = 0x01u
        private val regFDefault: UByte = 0xB0u
        private val regAFDefault: UShort = 0x01B0u

        private val regBDefault: UByte = 0x00u
        private val regCDefault: UByte = 0x13u
        private val regBCDefault: UShort = 0x0013u

        private val regDDefault: UByte = 0x00u
        private val regEDefault: UByte = 0xD8u
        private val regDEDefault: UShort = 0x00D8u

        private val regHDefault: UByte = 0x01u
        private val regLDefault: UByte = 0x4Du
        private val regHLDefault: UShort = 0x014Du

        @Test fun `read defaults as bytes`() {
            val registers = Cpu.Registers


        }
    }
}