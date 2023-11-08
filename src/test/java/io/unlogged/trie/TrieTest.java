package io.unlogged.trie;

import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.util.Iterator;

public class TrieTest {

    @Test
    public void trieTest() {
        InvertedRadixTree<Boolean> invertedRadixTree = new ConcurrentInvertedRadixTree<>(
                new DefaultCharArrayNodeFactory());

        invertedRadixTree.put("org.package1.is", false);
        invertedRadixTree.put("org.package2.is", false);
        invertedRadixTree.put("org.package3.is", false);
        invertedRadixTree.put("org.package4.is", false);
        invertedRadixTree.put("org.package5.is", false);
        invertedRadixTree.put("org.package6.is", false);

        Iterable<CharSequence> result = invertedRadixTree.getKeysPrefixing("org.package4.is.not.a.real");
        Iterator<CharSequence> iterator = result.iterator();
        Assertions.assertTrue(iterator.hasNext());
        CharSequence next = iterator.next();
        System.out.println("Match:  " + next);
        Assertions.assertEquals(next.toString(), "org.package4.is");

        Iterable<CharSequence> result1 = invertedRadixTree.getKeysPrefixing("org.pacage4.is.not.a.real");
        Iterator<CharSequence> iterator1 = result1.iterator();
        Assertions.assertFalse(iterator1.hasNext());
    }
}
