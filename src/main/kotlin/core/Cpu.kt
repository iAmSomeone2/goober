package core

@ExperimentalUnsignedTypes
object Cpu {
    object Registers {

        private val registerMap = mutableMapOf<Char, UByte>()
        var stackPointer: UShort = 0x0000u
        var programCounter: UShort = 0x0000u

        init {
            // Set the startup register values here.
            registerMap['a'] = 0x01u
            registerMap['f'] = 0xB0u
            registerMap['b'] = 0x00u
            registerMap['c'] = 0x13u
            registerMap['d'] = 0x00u
            registerMap['e'] = 0xD8u
            registerMap['h'] = 0x01u
            registerMap['l'] = 0x4Du

            stackPointer = 0xFFFEu
        }

        /**
         * Sets a single-byte register
         * @param reg the register to write to
         * @param data the data to write to the register
         */
        fun setRegister(reg: Char, data: UByte) {
            registerMap[reg] = data
        }

        /**
         * Sets a two-byte register combo
         * @param reg the register pair to write to
         * @param data the data to write to the register
         */
        fun setRegister(reg: String, data: UShort) {
            when (reg) {
                "af","bc","de","hl" -> {
                    // Take the upper-byte and turn it into its own Ubyte
                    val upperByte = (data and 0xFF00u).toUInt().shr(8).toUByte()
                    val lowerByte = (data and 0x00FFu).toUByte()
                    registerMap[reg[0]] = upperByte
                    registerMap[reg[1]] = lowerByte
                }

                else -> error("Tried to set a register that doesn't exist: $reg")
            }
        }

        /**
         * Reads a single-byte from a register
         * @param reg the register to read from
         * @return a UByte containing the data from the register.
         */
        fun readRegister(reg: Char): UByte {
            return registerMap[reg] ?: 0x00u
        }

        /**
         * Reads a two-byte register combo.
         * @param reg the register combo to read from
         * @return a UShort containing the data from the register combo.
         */
        fun readRegister(reg: String): UShort {

            val regData: UShort

            when (reg) {
                "af", "bc", "de", "hl" -> {
                    // There's no reason these should return "null", but we're still prepared.
                    val upperByte: UByte = registerMap[reg[0]] ?: 0x00u
                    val lowerByte: UByte = registerMap[reg[1]] ?: 0x00u

                    var byteCombo: UInt = upperByte.toUInt().shl(8)
                    byteCombo += lowerByte
                    regData = (byteCombo and 0xFFFFu).toUShort() // We'll mask this just in case
                }

                else -> error("Tried to read from a register that doesn't exist: $reg")
            }

            return regData
        }
    }
}