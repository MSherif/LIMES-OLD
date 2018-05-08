/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.execution.rewriter;

import de.uni_leipzig.simba.specification.LinkSpec;

/**
 *
 * @author ngonga
 */
public interface Rewriter {
    public LinkSpec rewrite(LinkSpec spec);
}
