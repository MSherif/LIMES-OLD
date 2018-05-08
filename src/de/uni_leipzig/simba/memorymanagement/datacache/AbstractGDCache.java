/**
 *
 */
package de.uni_leipzig.simba.memorymanagement.datacache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author mofeed
 * @author rspeck
 */
public abstract class AbstractGDCache extends AbstractCache {

    protected Double m_cacheAge = 0D;
    protected Map<Object, GDElement> m_accessMap = new LinkedHashMap<Object, GDElement>();

    public AbstractGDCache(int size, int evictCount) {
        super(size, evictCount);
        // TODO Auto-generated constructor stub
    }

    protected class GDElement implements Comparable<GDElement> {

        protected Double m_credit = 0D;

        /**
         * compare credit
         */
        public int compareTo(GDElement element) {
            return m_credit.compareTo(element.getCredit());
        }

        protected Double getCredit() {
            return m_credit;
        }

        /**
         * credit = age + special
         */
        protected void update(Double special) {
            m_credit = m_cacheAge + special;
        }
    }

}
