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

        //TODO: Figure out why checking the defaults passes if they are run on their own but fail otherwise.

        @Test fun `read defaults as bytes`() {
            val registers = Cpu.Registers

            assertEquals(regADefault, registers.read('a'))
            assertEquals(regFDefault, registers.read('f'))
            assertEquals(regBDefault, registers.read('b'))
            assertEquals(regCDefault, registers.read('c'))
            assertEquals(regDDefault, registers.read('d'))
            assertEquals(regEDefault, registers.read('e'))
            assertEquals(regHDefault, registers.read('h'))
            assertEquals(regLDefault, registers.read('l'))
        }

        @Test fun `read defaults as shorts`() {
            val registers = Cpu.Registers

            assertEquals(regAFDefault, registers.read("af"))
            assertEquals(regBCDefault, registers.read("bc"))
            assertEquals(regDEDefault, registers.read("de"))
            assertEquals(regHLDefault, registers.read("hl"))
        }

        @Test fun `write byte to register and read back`() {
            val expectedByte: UByte = 0xEAu
            val registers = Cpu.Registers

            registers.write('a', expectedByte)
            registers.write('f', expectedByte)
            registers.write('b', expectedByte)
            registers.write('c', expectedByte)
            registers.write('d', expectedByte)
            registers.write('e', expectedByte)
            registers.write('h', expectedByte)
            registers.write('l', expectedByte)

            assertEquals(expectedByte, registers.read('a'))
            assertEquals(expectedByte, registers.read('f'))
            assertEquals(expectedByte, registers.read('b'))
            assertEquals(expectedByte, registers.read('c'))
            assertEquals(expectedByte, registers.read('d'))
            assertEquals(expectedByte, registers.read('e'))
            assertEquals(expectedByte, registers.read('h'))
            assertEquals(expectedByte, registers.read('l'))
        }

        @Test fun `write short to register and read back`() {
            val expectedShort: UShort = 0xEDDEu
            val registers = Cpu.Registers

            registers.write("af", expectedShort)
            registers.write("bc", expectedShort)
            registers.write("de", expectedShort)
            registers.write("hl", expectedShort)

            assertEquals(expectedShort, registers.read("af"))
            assertEquals(expectedShort, registers.read("bc"))
            assertEquals(expectedShort, registers.read("de"))
            assertEquals(expectedShort, registers.read("hl"))
        }

        @Test fun `set and get stackPointer`() {
            val spVal: UShort = 0x23ABu
            val registers = Cpu.Registers

            registers.setStackPointer(spVal)
            assertEquals(spVal, registers.getStackPointer())
        }

        @Test fun `set and get programCounter`() {
            val pcVal: UShort = 0x4572u
            val registers = Cpu.Registers

            registers.setProgramCounter(pcVal)
            assertEquals(pcVal, registers.getProgramCounter())
        }
    }
}