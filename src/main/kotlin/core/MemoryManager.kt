package core

@ExperimentalUnsignedTypes
/**
 * Controls all memory access for the GameBoy CPU. Each logical section of the GameBoy's address space is separated
 * out into its own sub-object, so that the architecture will be easier to manage and debug.
 */
object MemoryManager {

    /**
     * The address space mapped to the game cartridge or other programs. This section is divided up into two sub-
     * sections, logically: 16KB ROM bank 00 and 16KB ROM bank 01~NN. The latter of these can be switched out with other
     * banks on the cartridge to allow for running programs larger than the GB's address space allows.
     */
    private object Rom {
        // TODO: Figure out a good way to gracefully load ROM on emu load
        private val bank00 = UByteArray(16_384)
        private val bankEX = UByteArray(16_384)

        fun read(addr: UShort): UByte {
            return if (addr <= 0x3FFFu) {
                // Read from bank00
                bank00[addr.toInt()]
            } else {
                val location = (addr - 0x4000u).toInt()
                bankEX[location]
            }
        }
    }

    /**
     * 8KB of address space reserved for video display.
     */
    private object VideoRAM {
        private val vRAM = UByteArray(8192)

        fun read(addr: UShort): UByte {
            val location = (addr - 0x8000u).toInt()
            return vRAM[location]
        }

        fun write(addr: UShort, data: UByte) {
            val location = (addr - 0x8000u).toInt()
            vRAM[location] = data
        }
    }

    /**
     * 8KB of bank-switchable RAM located on the cartridge.
     */
    private object ExternalRAM {
        private val exRAM = UByteArray(8192)

        fun read(addr: UShort): UByte {
            val location = (addr - 0xA000u).toInt()
            return exRAM[location]
        }

        fun write(addr: UShort, data: UByte) {
            val location = (addr - 0xA000u).toInt()
            exRAM[location] = data
        }
    }

    /**
     * 8KB of working RAM located in the GameBoy. The latter 4KB are bank-switchable in GameBoy Color mode (if that
     * ever gets implemented). ECHO RAM is also included in case it's needed.
     */
    private object WorkRAM {
        private val bank0 = UByteArray(4096)
        private val bankN = UByteArray(4096)
        private val echo = UByteArray(7680)

        fun read(addr: UShort): UByte {
            val data: UByte

            if (addr >= 0xC000u && addr <= 0xCFFFu) {
                // Bank 0
                val location = (addr - 0xC000u).toInt()
                data = bank0[location]
            } else if (addr >= 0xD000u && addr <= 0xDFFFu) {
                // Bank N
                val location = (addr - 0xD000u).toInt()
                data = bankN[location]
            } else {
                // Echo
                val location = (addr - 0xE000u).toInt()
                data = echo[location]
            }

            return data
        }

        fun write(addr: UShort, data: UByte) {
            TODO() // Take the echo into account
        }
    }

    /**
     * Address space for managing game sprites.
     */
    private object SpriteAttribTable {
        private val oam = UByteArray(160)

        fun read(addr: UShort): UByte {
            val location = (addr - 0xFE00u).toInt()
            return oam[location]
        }

        fun write(addr: UShort, data: UByte) {
            val location = (addr - 0xFE00u).toInt()
            oam[location] = data
        }
    }

    /**
     * Address space reserved for IO data.
     */
    private object IORegisters {
        private val ioRegs = UByteArray(128)

        fun read(addr: UShort): UByte {
            val location = (addr - 0xFF00u).toInt()
            return ioRegs[location]
        }

        fun write(addr: UShort, data: UByte) {
            val location = (addr - 0xFF00u).toInt()
            ioRegs[location] = data
        }
    }

    // High RAM and the Interrupt Enable Register are directly controlled by the MemoryManager.

    private val highRAM = UByteArray(126)
    private val intEnableReg: UByte = 0x00u         // Interrupt Enable Register

    private val rom = Rom
    private val vRam = VideoRAM
    private val exRam = ExternalRAM
    private val workRam = WorkRAM
    private val spriteTab = SpriteAttribTable
    private val ioReg = IORegisters
}