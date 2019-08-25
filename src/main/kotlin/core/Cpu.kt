package core

@ExperimentalUnsignedTypes
object Cpu {
    /**
     * A sub-object of the CPU used for maintaining register states.
     */
    object Registers {

        private val registerMap = mutableMapOf<Char, UByte>()
        private var stackPointer: UShort = 0x0000u
        private var programCounter: UShort = 0x0000u

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

        // Basic getters and setters

        fun getStackPointer(): UShort {
            return stackPointer
        }

        fun setStackPointer(data: UShort) {
            stackPointer = data
        }

        fun getProgramCounter(): UShort {
            return programCounter
        }

        fun setProgramCounter(data: UShort) {
            programCounter = data
        }

        /**
         * Sets a single-byte register
         * @param reg the register to write to
         * @param data the data to write to the register
         */
        fun write(reg: Char, data: UByte) {
            registerMap[reg] = data
        }

        /**
         * Sets a two-byte register combo
         * @param reg the register pair to write to
         * @param data the data to write to the register
         */
        fun write(reg: String, data: UShort) {
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
        fun read(reg: Char): UByte {
            return registerMap[reg] ?: 0x00u
        }

        /**
         * Reads a two-byte register combo.
         * @param reg the register combo to read from
         * @return a UShort containing the data from the register combo.
         */
        fun read(reg: String): UShort {

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

    private val registers = Registers
    private val memory = MemoryManager

    /**
     * Loads a value from one register into another
     * @param outReg the register to write to
     * @param inReg the register to read from
     * @return number of ticks for operation to complete
     */
    private fun load8BitReg(outReg: Char, inReg: Char): Int {
        TODO()
    }

    private fun load8BitVal(output: String, input: String) {
        TODO()
    }

    /**
     * Reads the value stored in the memory location that register BC points to and loads it into register A.
     * @return number of ticks for operation to complete
     */
    private fun loadToAFromBCPtr(): Int {
        val location: UShort = registers.read("bc")
        registers.write('a', memory.readByte(location))
        return 8
    }

    /**
     * Reads the value stored in the memory location that register DE points to and loads it into register A.
     * @return number of ticks for operation to complete
     */
    private fun loadToAFromDEPtr(): Int {
        val location: UShort = registers.read("de")
        registers.write('a', memory.readByte(location))
        return 8
    }

    /**
     * Reads the value stored in the memory location that the value of n points to and loads it into register A.
     * @param n memory address to read from
     * @return number of ticks for operation to complete
     */
    private fun loadToAFromNPtr(n: UByte): Int {
        TODO()
        // return 16
    }

    /**
     * Reads the value stored in register A and writes it to the memory location pointed to by register BC.
     * @return number of ticks for operation to complete
     */
    private fun loadToBCPtrFromA(): Int {
        val location: UShort = registers.read("bc")
        memory.writeByte(location, registers.read('a'))
        return 8
    }

    /**
     * Reads the value stored in register A and writes it to the memory location pointed to by register DE.
     * @return number of ticks for operation to complete
     */
    private fun loadToDEPtrFromA(): Int {
        val location: UShort = registers.read("de")
        memory.writeByte(location, registers.read('a'))
        return 8
    }

    /**
     * Reads the value stored in register A and writes it to the memory location pointed to by n.
     * @param n memory address to write to
     * @return number of ticks for operation to complete
     */
    private fun loadToNPtrFromA(n: UByte): Int {
        TODO()
        //return 16
    }

    /**
     * Loads the value from the I/O port specified into register A.
     * @param port port number to read from
     * @return number of ticks for operation to complete
     */
    private fun readIO(port: UByte): Int {
        // Do not forget to add FF00 to the port number.
        val location: UShort = (0xFF00u + port).toUShort()
        registers.write('a', memory.readByte(location))
        return 12
    }

    /**
     * Loads the value from the I/O port specified by register C into register A.
     * @return number of ticks for operation to complete
     */
    private fun readIO(): Int {
        // Do not forget to add FF00 to the value from C
        val location: UShort = (0xFF00u + registers.read('c')).toUShort()
        registers.write('a', memory.readByte(location))
        return 8
    }

    /**
     * Writes the contents of register A to the specified I/O port
     * @param port port number to write to
     * @return number of ticks for operation to complete
     */
    private fun writeIO(port: UByte): Int {
        val location: UShort = (0xFF00u + port).toUShort()
        memory.writeByte(location, registers.read('a'))
        return 12
    }

    /**
     * Writes the contents of register A to the I/O port specified by register C
     * @return number of ticks for operation to complete
     */
    private fun writeIO(): Int {
        val location: UShort = (0xFF00u + registers.read('c')).toUShort()
        memory.writeByte(location, registers.read('a'))
        return 8
    }
}