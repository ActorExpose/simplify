package org.cf.smalivm.opcode

import org.cf.smalivm.configuration.Configuration
import org.cf.smalivm.dex.CommonTypes
import org.cf.smalivm.dex.SmaliClassLoader
import org.cf.smalivm.type.ClassManager
import org.cf.smalivm2.ExecutionNode
import org.cf.smalivm2.UnresolvedChild
import org.cf.smalivm2.Value
import org.jf.dexlib2.builder.BuilderInstruction
import org.jf.dexlib2.builder.MethodLocation
import org.jf.dexlib2.iface.instruction.OffsetInstruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction22t
import org.slf4j.LoggerFactory

class IfOp internal constructor(
    location: MethodLocation,
    private val ifType: IfType,
    private val targetAddress: Int,
    private val register1: Int
) : Op(location) {
    private var compareToZero = true
    private var register2 = 0

    internal constructor(location: MethodLocation, ifType: IfType, targetAddress: Int, register1: Int, register2: Int) : this(
        location,
        ifType,
        targetAddress,
        register1
    ) {
        this.register2 = register2
        compareToZero = false
    }

    override val registersReadCount = if (compareToZero) 1 else 2
    override val registersAssignedCount = 1

    override fun execute(node: ExecutionNode): Array<out UnresolvedChild> {
        val lhs = node.state.readRegister(register1)
        val rhs = if (compareToZero) Value.wrap(0, CommonTypes.INTEGER) else node.state.readRegister(register2)

        // Ambiguous predicate. Return to add both possible branches as children.
        if (lhs.isUnknown || rhs.isUnknown) {
            return finishOp(arrayOf(nextAddress, targetAddress))
        }
        val cmp = if (compareToZero) {
            if (lhs.value == null) {
                // if-*z ops are used to check for null refs
                0
            } else if ((lhs.value is Number || lhs.value is Boolean || lhs.value is Char) &&
                (rhs.value is Number || rhs.value is Boolean || rhs.value is Char)
            ) {
                val aIntValue = lhs.toInteger()
                aIntValue.compareTo(rhs.value as Int)
            } else {
                if (lhs.value === rhs.value) 0 else 1
            }
        } else if ((lhs.value is Number || lhs.value is Boolean || lhs.value is Char) &&
            (rhs.value is Number || rhs.value is Boolean || rhs.value is Char)
        ) {
            val aIntValue = lhs.toInteger()
            val bIntValue = rhs.toInteger()
            aIntValue.compareTo(bIntValue)
        } else {
            if (lhs.value === rhs.value) 0 else 1
        }

        log.trace("IF compare: {} vs {} = {}", lhs.value, rhs.value, cmp)
        val childAddress = when {
            isTrue(ifType, cmp) -> targetAddress
            else -> nextAddress
        }
        return finishOp(childAddress)
    }

    override fun toString(): String {
        val sb = StringBuilder(name)
        sb.append(" r").append(register1)
        if (!compareToZero) {
            sb.append(", r").append(register2)
        }
        sb.append(", :addr_").append(targetAddress)
        return sb.toString()
    }

    internal enum class IfType {
        EQUAL, GREATER, GREATER_OR_EQUAL, LESS, LESS_OR_EQUAL, NOT_EQUAL
    }

    companion object : OpFactory {
        private val log = LoggerFactory.getLogger(IfOp::class.java.simpleName)
        private fun isTrue(ifType: IfType?, cmp: Int): Boolean {
            var isTrue = false
            when (ifType) {
                IfType.EQUAL -> isTrue = cmp == 0
                IfType.GREATER -> isTrue = cmp == 1
                IfType.GREATER_OR_EQUAL -> isTrue = cmp >= 0
                IfType.LESS -> isTrue = cmp == -1
                IfType.LESS_OR_EQUAL -> isTrue = cmp <= 0
                IfType.NOT_EQUAL -> isTrue = cmp != 0
            }
            return isTrue
        }

        override fun build(
            location: MethodLocation,
            classManager: ClassManager,
            classLoader: SmaliClassLoader,
            configuration: Configuration
        ): Op {
            val instruction = location.instruction as BuilderInstruction
            val address = instruction.location.codeAddress
            val branchOffset = (instruction as OffsetInstruction?)!!.codeOffset
            val targetAddress = address + branchOffset
            val opName = instruction.getOpcode().name
            val ifType = getIfType(opName)
            val register1 = (instruction as OneRegisterInstruction?)!!.registerA
            return if (instruction is Instruction22t) {
                // if-* vA, vB, :label
                IfOp(location, ifType, targetAddress, register1, instruction.registerB)
            } else {
                // if-*z vA, :label (Instruction 21t)
                IfOp(location, ifType, targetAddress, register1)
            }
        }

        private fun getIfType(opName: String): IfType {
            return when {
                opName.contains("-eq") -> IfType.EQUAL
                opName.contains("-ne") -> IfType.NOT_EQUAL
                opName.contains("-lt") -> IfType.LESS
                opName.contains("-le") -> IfType.LESS_OR_EQUAL
                opName.contains("-gt") -> IfType.GREATER
                opName.contains("-ge") -> IfType.GREATER_OR_EQUAL
                else -> throw RuntimeException("Unrecognized IF opcode type: $opName")
            }
        }
    }
}
