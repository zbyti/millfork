package millfork.assembly.z80.opt

import millfork.assembly.AssemblyOptimization
import millfork.assembly.z80._
import millfork.node.ZRegister
import ZOpcode._
import ZRegister._


/**
  * @author Karol Stasiak
  */
object LaterI80Optimizations {
  val VariousSmallOptimizations = new RuleBasedAssemblyOptimization("Various small optimizations",
    needsFlowInfo = FlowInfoRequirement.BackwardFlow,

    (Elidable & Is8BitLoadTo(A) & Has8BitImmediate(0) & DoesntMatterWhatItDoesWithFlags) ~~> { _ =>
      List(ZLine.register(XOR, A))
    },
    (Elidable & HasOpcode(CP) & DoesntMatterWhatItDoesWithFlags) ~~> { _ =>
      Nil
    },
    (Elidable & HasOpcode(CP) & Has8BitImmediate(0) & DoesntMatterWhatItDoesWithFlagsOtherThanSZ) ~~> { _ =>
      List(ZLine.register(OR, A))
    },
    (Elidable & HasOpcode(CP) & Has8BitImmediate(1) & DoesntMatterWhatItDoesWithFlagsOtherThanSZ & DoesntMatterWhatItDoesWith(A)) ~~> { _ =>
      List(ZLine.register(DEC, A))
    },
    (Elidable & HasOpcode(CP) & Has8BitImmediate(255) & DoesntMatterWhatItDoesWithFlagsOtherThanSZ & DoesntMatterWhatItDoesWith(A)) ~~> { _ =>
      List(ZLine.register(INC, A))
    },

    (Elidable & HasOpcodeIn(Set(JP, JR)) & HasRegisters(IfFlagClear(ZFlag.C)) & MatchParameter(0)) ~
      (Elidable & HasOpcode(INC) & HasRegisterParam(A) & DoesntMatterWhatItDoesWithFlags) ~
      (HasOpcode(LABEL) & MatchParameter(0)) ~~> (code => List(ZLine.imm8(ADC, 0), code.last)),

    (Elidable & HasOpcodeIn(Set(JP, JR)) & HasRegisters(IfFlagSet(ZFlag.C)) & MatchParameter(0)) ~
      (Elidable & HasOpcode(INC) & HasRegisterParam(A) & DoesntMatterWhatItDoesWithFlags) ~
      (HasOpcode(LABEL) & MatchParameter(0)) ~~> (code => List(ZLine.implied(CCF), ZLine.imm8(ADC, 0), code.last)),

    (Elidable & HasOpcodeIn(Set(JP, JR)) & HasRegisters(IfFlagClear(ZFlag.C)) & MatchParameter(0)) ~
      (Elidable & HasOpcode(DEC) & HasRegisterParam(A) & DoesntMatterWhatItDoesWithFlags) ~
      (HasOpcode(LABEL) & MatchParameter(0)) ~~> (code => List(ZLine.imm8(SBC, 0), code.last)),

    (Elidable & HasOpcodeIn(Set(JP, JR)) & HasRegisters(IfFlagSet(ZFlag.C)) & MatchParameter(0)) ~
      (Elidable & HasOpcode(DEC) & HasRegisterParam(A) & DoesntMatterWhatItDoesWithFlags) ~
      (HasOpcode(LABEL) & MatchParameter(0)) ~~> (code => List(ZLine.implied(CCF), ZLine.imm8(SBC, 0), code.last)),

  )

  val FreeHL = new RuleBasedAssemblyOptimization("Free HL (later)",
    needsFlowInfo = FlowInfoRequirement.BackwardFlow,

    (Elidable & Is8BitLoad(H, B)) ~
      (Elidable & Is8BitLoad(L, C)) ~
      (Elidable & Is8BitLoad(MEM_HL, A) & DoesntMatterWhatItDoesWith(B, C)) ~~> { _ =>
      List(ZLine.ld8(MEM_BC, A))
    },
    (Elidable & Is8BitLoad(H, B)) ~
      (Elidable & Is8BitLoad(L, C)) ~
      (Elidable & Is8BitLoad(A, MEM_HL) & DoesntMatterWhatItDoesWith(B, C)) ~~> { _ =>
      List(ZLine.ld8(A, MEM_BC))
    },
    (Elidable & Is8BitLoad(H, D)) ~
      (Elidable & Is8BitLoad(L, E)) ~
      (Elidable & Is8BitLoad(MEM_DE, A) & DoesntMatterWhatItDoesWith(D, E)) ~~> { _ =>
      List(ZLine.ld8(MEM_DE, A))
    },
    (Elidable & Is8BitLoad(H, D)) ~
      (Elidable & Is8BitLoad(L, E)) ~
      (Elidable & Is8BitLoad(A, MEM_HL) & DoesntMatterWhatItDoesWith(D, E)) ~~> { _ =>
      List(ZLine.ld8(A, MEM_DE))
    },
  )

  val All: List[AssemblyOptimization[ZLine]] = List(
    VariousSmallOptimizations,
    FreeHL
  )
}