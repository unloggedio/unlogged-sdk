package io.unlogged.logging.util;

/**
 * Same as IntIntMap3, but now we merge keys and values into one long[], where
 * lower 32 bits contain key (because they are cheaper to extract) and upper 32 bits contain value.
 */
public class IntIntMap4 implements IntIntMap
{
    private static final int FREE_KEY = 0;
    private static final long FREE_CELL = 0;

    private static long KEY_MASK = 0xFFFFFFFFL;

    public static final int NO_VALUE = 0;

    /** Keys and values */
    private long[] m_ar;

    /** Do we have 'free' key in the map? */
    private boolean m_hasFreeKey;
    /** Value of 'free' key */
    private int m_freeValue;

    /** Fill factor, must be between (0 and 1) */
    private final float m_fillFactor;
    /** We will resize a map once it reaches this size */
    private int m_threshold;
    /** Current map size */
    private int m_size;
    /** Mask to calculate the original position */
    private int m_mask;

    public IntIntMap4( final int size, final float fillFactor )
    {
        if ( fillFactor <= 0 || fillFactor >= 1 )
            throw new IllegalArgumentException( "FillFactor must be in (0, 1)" );
        if ( size <= 0 )
            throw new IllegalArgumentException( "Size must be positive!" );
        final int capacity = Tools.arraySize( size, fillFactor );
        m_mask = capacity - 1;
        m_fillFactor = fillFactor;

        m_ar = new long[capacity];
        m_threshold = (int) (capacity * fillFactor);
    }

    public int get( final int key )
    {
        if ( key == FREE_KEY )
            return m_hasFreeKey ? m_freeValue : NO_VALUE;

        int idx = getStartIndex(key);
        long c = m_ar[ idx ];
        if ( c == FREE_CELL )
            return NO_VALUE;  //end of chain already
        if ( ((int)(c & KEY_MASK)) == key ) //we check FREE prior to this call
            return (int) (c >> 32);
        while ( true )
        {
            idx = getNextIndex(idx);
            c = m_ar[ idx ];
            if ( c == FREE_CELL )
                return NO_VALUE;
            if ( ((int)(c & KEY_MASK)) == key )
                return (int) (c >> 32);
        }
    }

    public int put( final int key, final int value )
    {
        if ( key == FREE_KEY )
        {
            final int ret = m_freeValue;
            if ( !m_hasFreeKey )
                ++m_size;
            m_hasFreeKey = true;
            m_freeValue = value;
            return ret;
        }

        int idx = getStartIndex( key );
        long c = m_ar[idx];
        if ( c == FREE_CELL ) //end of chain already
        {
            m_ar[ idx ] = (((long)key) & KEY_MASK) | ( ((long)value) << 32 );
            if ( m_size >= m_threshold )
                rehash( m_ar.length * 2 ); //size is set inside
            else
                ++m_size;
            return NO_VALUE;
        }
        else if ( ((int)(c & KEY_MASK)) == key ) //we check FREE prior to this call
        {
            m_ar[ idx ] = (((long)key) & KEY_MASK) | ( ((long)value) << 32 );
            return (int) (c >> 32);
        }

        while ( true )
        {
            idx = getNextIndex( idx );
            c = m_ar[ idx ];
            if ( c == FREE_CELL )
            {
                m_ar[ idx ] = (((long)key) & KEY_MASK) | ( ((long)value) << 32 );
                if ( m_size >= m_threshold )
                    rehash( m_ar.length * 2 ); //size is set inside
                else
                    ++m_size;
                return NO_VALUE;
            }
            else if ( ((int)(c & KEY_MASK)) == key )
            {
                m_ar[ idx ] = (((long)key) & KEY_MASK) | ( ((long)value) << 32 );
                return (int) (c >> 32);
            }
        }
    }

    public int remove( final int key )
    {
        if ( key == FREE_KEY )
        {
            if ( !m_hasFreeKey )
                return NO_VALUE;
            m_hasFreeKey = false;
            final int ret = m_freeValue;
            m_freeValue = NO_VALUE;
            --m_size;
            return ret;
        }

        int idx = getStartIndex( key );
        long c = m_ar[ idx ];
        if ( c == FREE_CELL )
            return NO_VALUE;  //end of chain already
        if ( ((int)(c & KEY_MASK)) == key ) //we check FREE prior to this call
        {
            --m_size;
            shiftKeys( idx );
            return (int) (c >> 32);
        }
        while ( true )
        {
            idx = getNextIndex( idx );
            c = m_ar[ idx ];
            if ( c == FREE_CELL )
                return NO_VALUE;
            if ( ((int)(c & KEY_MASK)) == key )
            {
                --m_size;
                shiftKeys( idx );
                return (int) (c >> 32);
            }
        }
    }

    public int size()
    {
        return m_size;
    }

    private int shiftKeys(int pos)
    {
        // Shift entries with the same hash.
        int last, slot;
        int k;
        final long[] data = this.m_ar;
        while ( true )
        {
            pos = ((last = pos) + 1) & m_mask;
            while ( true )
            {
                if ((k = ((int)(data[pos] & KEY_MASK))) == FREE_KEY)
                {
                    data[last] = FREE_CELL;
                    return last;
                }
                slot = getStartIndex(k); //calculate the starting slot for the current key
                if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
                pos = (pos + 1) & m_mask; //go to the next entry
            }
            data[last] = data[pos];
        }
    }

    private void rehash( final int newCapacity )
    {
        m_threshold = (int) (newCapacity * m_fillFactor);
        m_mask = newCapacity - 1;

        final int oldCapacity = m_ar.length;
        final long[] oldData = m_ar;

        m_ar = new long[ newCapacity ];
        m_size = m_hasFreeKey ? 1 : 0;

        for ( int i = oldCapacity; i-- > 0; ) {
            final int oldKey = (int) (oldData[ i ] & KEY_MASK);
            if( oldKey != FREE_KEY )
                put( oldKey, (int) (oldData[ i ] >> 32));
        }
    }
    private int getStartIndex( final int key )
    {
        return Tools.phiMix(key) & m_mask;
    }

    private int getNextIndex( final int currentIndex )
    {
        return ( currentIndex + 1 ) & m_mask;
    }


}