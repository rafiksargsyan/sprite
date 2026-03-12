package com.rsargsyan.sprite.main_ctx.core;

import com.rsargsyan.sprite.main_ctx.core.exception.InvalidIdException;
import io.hypersistence.tsid.TSID;

public class Util {
  public static Long validateTSID(String tsid) {
    if (TSID.isValid(tsid)) return TSID.from(tsid).toLong();
    throw new InvalidIdException();
  }
}
