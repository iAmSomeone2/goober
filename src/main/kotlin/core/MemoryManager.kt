package core

import java.math.RoundingMode
import java.nio.file.FileSystems


@ExperimentalUnsignedTypes
/**
 * Controls all memory access for the GameBoy CPU. Each logical section of the GameBoy's address space is separated
 * out into its own sub-object, so that the architecture will be easier to manage and debug.
 */
object MemoryManager {
    // TODO: Set up functionality to export all memory as a save state.

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
        private val exRamBanks = mutableListOf(
            UByteArray(8192),       // Bank 00
            UByteArray(8192),       // Bank 01
            UByteArray(8192),       // Bank 02
            UByteArray(8192),       // Bank 03
            UByteArray(8192),       // Bank 04
            UByteArray(8192),       // Bank 05
            UByteArray(8192),       // Bank 06
            UByteArray(8192),       // Bank 07
            UByteArray(8192),       // Bank 08
            UByteArray(8192),       // Bank 09
            UByteArray(8192),       // Bank 10
            UByteArray(8192),       // Bank 11
            UByteArray(8192),       // Bank 12
            UByteArray(8192),       // Bank 13
            UByteArray(8192),       // Bank 14
            UByteArray(8192)        // Bank 15
        )
        private var exRAM = UByteArray(8192)
        private var currentBank = 0

        // TODO: Set up functionality to export exRAM as a binary save file

        fun read(addr: UShort): UByte {
            val location = (addr - 0xA000u).toInt()
            return exRAM[location]
        }

        fun write(addr: UShort, data: UByte) {
            val location = (addr - 0xA000u).toInt()
            exRAM[location] = data
        }

        /**
         * Sets current working RAM bank. The current bank's data is saved to the corresponding section of
         * exRamBanks, and exRAM is replaced with the new bank.
         * @param bankNum the bank to switch to
         */
        fun setCurrentBank(bankNum: Int) {
            if (bankNum in 0..15) {
                // Save current values to the corresponding bank
                exRamBanks[currentBank] = exRAM
                // Swap banks
                exRAM = exRamBanks[bankNum]
                currentBank = bankNum
            } else {
                println("WARN: Tried to set exRAM bank to a bank that doesn't exist: $bankNum")
            }
        }

        /**
         * Returns an integer representing the current workng bank.
         * @return Int representing current working exRAM bank
         */
        fun getCurrentBank(): Int {
            return currentBank
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
            return when (addr) {
                in 0xC000u..0xCFFFu -> {
                    // Bank 0
                    val location = (addr - 0xC000u).toInt()
                    bank0[location]
                }
                in 0xD000u..0xDFFFu -> {
                    // Bank N
                    val location = (addr - 0xD000u).toInt()
                    bankN[location]
                }
                else -> {
                    // Echo
                    val location = (addr - 0xE000u).toInt()
                    echo[location]
                }
            }
        }

        /**
         * Helper function for handling ECHO RAM without getting into a loop.
         * @param addr location to write to.
         * @param data value to write
         */
        private fun echo(addr: UShort, data: UByte) {
            val location: Int
            when (addr) {
                in 0xC000u..0xDDFFu -> {
                    // Write into ECHO RAM
                    location = (addr-0xC000u).toInt()
                    echo[location] = data
                }
                in 0xE000u..0xEFFFu -> {
                    // Write into first bank
                    location = (addr-0xE000u).toInt()
                    bank0[location] = data
                }
                in 0xF000u..0xFDFFu -> {
                    // Write into switchable bank
                    location = (addr-0xF000u).toInt()
                    bankN[location] = data
                }
            }
        }

        /**
         * Writes a single-byte value to the specified address in work RAM.
         * ECHO RAM is taken into account and copied appropriately.
         * @param addr location to write to
         * @param data value to write
         */
        fun write(addr: UShort, data: UByte) {
            when (addr) {
                in 0xC000u..0xCFFFu -> {
                    // Bank 0
                    val location = (addr - 0xC000u).toInt()
                    bank0[location] = data
                    // Anything in this bank should be copied to echo
                    echo(addr, data)
                }
                in 0xD000u..0xDFFFu -> {
                    // Bank N
                    val location = (addr - 0xD000u).toInt()
                    bankN[location] = data

                    if (addr <= 0xDDFFu) {
                        // Copy anything from these addresses into echo
                        echo(addr, data)
                    }
                }
                in 0xE000u..0xFDFFu -> {
                    // Echo
                    val location = (addr - 0xE000u).toInt()
                    echo[location] = data
                    // Anything written here needs to be echoed into the address 0x2000 lower
                    echo(addr, data)
                }

                else -> error("Program tried to access invalid RAM @ ${addr.toUShort()}")
            }
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
    private var intEnableReg: UByte = 0x00u         // Interrupt Enable Register

    private val rom: Rom = Rom
    private val vRam: VideoRAM = VideoRAM
    private val exRam: ExternalRAM = ExternalRAM
    private val workRam: WorkRAM = WorkRAM
    private val spriteTab: SpriteAttribTable = SpriteAttribTable
    private val ioReg: IORegisters = IORegisters

    // TODO: Determine number of ExRAM banks to create based on what the ROM specifies.


    /**
     * Takes the ROM path as a string and converts it to a path before sending that information to the
     * ROM object so that it may load the program ROM into its banks.
     * @param romLoc location on disk to look for program ROM.
     */
    fun loadRomFromFile(romLoc: String) {
        val romPath = FileSystems.getDefault().getPath(romLoc)
        // TODO: Add code to deal with FileNotFound errors.
        rom.loadRom(romPath)
    }

    /**
     * Finds and returns the value from the specified memory address.
     * @param addr memory address to read from
     * @return UByte value from specified memory address
     */
    fun readByte(addr: UShort): UByte {
        return when (addr) {
            in 0x0000u..0x7FFFu -> {
                // Read from ROM
                rom.read(addr)
            }
            in 0x8000u..0x9FFFu -> {
                // Read from vRAM
                vRam.read(addr)
            }
            in 0xA000u..0xBFFFu -> {
                // Read from External RAM
                exRam.read(addr)
            }
            in 0xC000u..0xFDFFu -> {
                // Read from workRAM
                workRam.read(addr)
            }
            in 0xFE00u..0xFE9Fu -> {
                // Read from Sprite Attribute Table
                spriteTab.read(addr)
            }
            in 0xFEA0u..0xFEFFu -> {
                // Unusable address space
                println("WARN: Program tried to access unusable value @ ${addr.toUShort()}")
                0x00u
            }
            in 0xFF00u..0xFF7Fu -> {
                // Read from I/O
                ioReg.read(addr)
            }
            in 0xFF80u..0xFFFEu -> {
                // Read from High RAM (HRAM)
                val location = (addr - 0xFF80u).toInt()
                highRAM[location]
            }
            0xFFFFu.toUShort() -> {  // Sometimes Kotlin is weird about type inference
                intEnableReg
            }

            else -> error("Tried to access an address that doesn't exist: ${addr.toUShort()}")
        }
    }

    /**
     * Sets a byte in memory according to the specified memory address.
     * @param addr memory address to write to
     * @param data data to write to memory
     */
    fun writeByte(addr: UShort, data: UByte) {
        when (addr) {
            in 0x0000u..0x7FFFu -> {
                // ROM
                println("WARN: Program tried to write to ROM")
            }
            in 0x8000u..0x9FFFu -> {
                // Write to vRAM
                vRam.write(addr, data)
            }
            in 0xA000u..0xBFFFu -> {
                // Write to External RAM
                exRam.write(addr, data)
            }
            in 0xC000u..0xFDFFu -> {
                // Read from workRAM
                workRam.write(addr, data)
            }
            in 0xFE00u..0xFE9Fu -> {
                // Read from Sprite Attribute Table
                spriteTab.write(addr, data)
            }
            in 0xFEA0u..0xFEFFu -> {
                // Unusable address space
                println("WARN: Program tried to access unusable value @ ${addr.toUShort()}")
            }
            in 0xFF00u..0xFF7Fu -> {
                // Read from I/O
                ioReg.write(addr, data)
            }
            in 0xFF80u..0xFFFEu -> {
                // Write to High RAM (HRAM)
                val location = (addr - 0xFF80u).toInt()
                highRAM[location] = data
            }
            0xFFFFu.toUShort() -> {  // Sometimes Kotlin is weird about type inference
                intEnableReg = data
            }

            else -> error("Tried to access an address that doesn't exist: ${addr.toUShort()}")
        }
    }
}