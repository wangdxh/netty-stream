package com.wangdxh.handler;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;

class NalUnitBuf
{
    public int index;
    public int length;
}

public class H264FrameInfo
{
    ArrayList<NalUnitBuf> nallist = new ArrayList<>(3);
    final byte[] buffer;
    final int length;
    public H264FrameInfo(byte[] buf, int length)
    {
        this.buffer = buf;
        this.length = length;
        parseNalinfo();
    }

    public void toBytebuf(ByteBuf dst)
    {
        for (NalUnitBuf buf : nallist)
        {
            dst.writeInt(buf.length);
            dst.writeBytes(this.buffer, buf.index, buf.length);
        }
    }
    private void parseNalinfo()
    {
        nallist.clear();
        int dwLen = this.length;

        int i = dwLen;
        int syncPoint = 0;
        for (; syncPoint < dwLen - 3; syncPoint++)
        {
            if (buffer[syncPoint + 2] == 1)
            {
                // the sync point is properly aligned
                i = syncPoint + 5;
                break;
            }
        }

        while (i < dwLen)
        {
            // look at the current byte to determine if we've hit the end of
            // a NAL unit boundary
            switch (buffer[i])
            {
                case 0:
                    // skip past non-sync sequences
                    if (buffer[i - 1] != 0) {
                        i += 2;
                        break;
                    } else if (buffer[i - 2] != 0) {
                        i++;
                        break;
                    }

                    // deliver the NAL unit if it isn't empty
                    if (syncPoint + 3 != i - 2)
                    {
                        //this.trigger('data', buffer.subarray(syncPoint + 3, i - 2));
                        // one nalu
                        NalUnitBuf tNal = new NalUnitBuf();
                        tNal.index = syncPoint+3;
                        tNal.length = i-2 - (syncPoint + 3);
                        nallist.add(tNal);
                    }

                    // drop trailing zeroes
                    do
                    {
                        i++;
                    } while (buffer[i] != 1 && i < dwLen);
                    syncPoint = i - 2;
                    i += 3;
                    break;
                case 1:
                    // skip past non-sync sequences
                    if (buffer[i - 1] != 0 ||
                            buffer[i - 2] != 0)
                    {
                        i += 3;
                        break;
                    }

                    // deliver the NAL unit
                    //this.trigger('data', buffer.subarray(syncPoint + 3, i - 2));

                    NalUnitBuf tNal = new NalUnitBuf();
                    tNal.index = syncPoint+3;
                    tNal.length = i-2 - (syncPoint + 3);
                    nallist.add(tNal);

                    syncPoint = i - 2;
                    i += 3;
                    break;
                default:
                    // the current byte isn't a one or zero, so it cannot be part
                    // of a sync sequence
                    i += 3;
                    break;
            }
        }
        // filter out the NAL units that were delivered
        //buffer = buffer.subarray(syncPoint);
        if (syncPoint < dwLen)
        {
            NalUnitBuf tNal = new NalUnitBuf();
            tNal.index= syncPoint+3;
            tNal.length = dwLen - syncPoint - 3;
            nallist.add(tNal);
        }
        i -= syncPoint;
        syncPoint = 0;
    }
}
