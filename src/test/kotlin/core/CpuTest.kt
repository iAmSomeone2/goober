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

            assertEquals(regADefault, registers.readRegister('a'))
            assertEquals(regFDefault, registers.readRegister('f'))
            assertEquals(regBDefault, registers.readRegister('b'))
            assertEquals(regCDefault, registers.readRegister('c'))
            assertEquals(regDDefault, registers.readRegister('d'))
            assertEquals(regEDefault, registers.readRegister('e'))
            assertEquals(regHDefault, registers.readRegister('h'))
            assertEquals(regLDefault, registers.readRegister('l'))
        }

        @Test fun `read defaults as shorts`() {
            val registers = Cpu.Registers

            assertEquals(regAFDefault, registers.readRegister("af"))
            assertEquals(regBCDefault, registers.readRegister("bc"))
            assertEquals(regDEDefault, registers.readRegister("de"))
            assertEquals(regHLDefault, registers.readRegister("hl"))
        }

        @Test fun `write byte to register and read back`() {
            val expectedByte: UByte = 0xEAu
            val registers = Cpu.Registers

            registers.setRegister('a', expectedByte)
            registers.setRegister('f', expectedByte)
            registers.setRegister('b', expectedByte)
            registers.setRegister('c', expectedByte)
            registers.setRegister('d', expectedByte)
            registers.setRegister('e', expectedByte)
            registers.setRegister('h', expectedByte)
            registers.setRegister('l', expectedByte)

            assertEquals(expectedByte, registers.readRegister('a'))
            assertEquals(expectedByte, registers.readRegister('f'))
            assertEquals(expectedByte, registers.readRegister('b'))
            assertEquals(expectedByte, registers.readRegister('c'))
            assertEquals(expectedByte, registers.readRegister('d'))
            assertEquals(expectedByte, registers.readRegister('e'))
            assertEquals(expectedByte, registers.readRegister('h'))
            assertEquals(expectedByte, registers.readRegister('l'))
        }

        @Test fun `write short to register and read back`() {
            val expectedShort: UShort = 0xEDDEu
            val registers = Cpu.Registers

            registers.setRegister("af", expectedShort)
            registers.setRegister("bc", expectedShort)
            registers.setRegister("de", expectedShort)
            registers.setRegister("hl", expectedShort)

            assertEquals(expectedShort, registers.readRegister("af"))
            assertEquals(expectedShort, registers.readRegister("bc"))
            assertEquals(expectedShort, registers.readRegister("de"))
            assertEquals(expectedShort, registers.readRegister("hl"))
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