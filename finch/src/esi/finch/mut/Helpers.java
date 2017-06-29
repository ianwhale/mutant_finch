package esi.finch.mut;

import org.objectweb.asm.tree.*;
import java.util.HashMap;
import java.util.Map;

public class Helpers {
    /**
     * Cloning an instruction requires the labels to be copied (if they have any).
     * @author https://github.com/NOVA-Team/NOVA-Core
     *         ExE-Boss@github.com, calclavia@github.com
     * @param method 
     * @return labelMap 
     */
    public static Map<LabelNode, LabelNode> cloneLabels(MethodNode method) {
        return cloneLabels(method.instructions);
    }

    /**
     * Clone using instruction list. 
     * @param insns
     * @return labelMap
     */
    public static Map<LabelNode, LabelNode> cloneLabels(InsnList insns) {
        HashMap<LabelNode, LabelNode> labelMap = new HashMap<LabelNode, LabelNode>();
        for (AbstractInsnNode node = insns.getFirst(); node != null; node = node.getNext()) {
            if (node.getType() == AbstractInsnNode.LABEL) {
                labelMap.put((LabelNode)node, new LabelNode());
            }
        }

        return labelMap;
    }
}
