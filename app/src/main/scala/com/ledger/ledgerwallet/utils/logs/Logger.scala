/**
 *
 * ${FILE_NAME}
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 09/01/15.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Ledger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.ledger.ledgerwallet.utils.logs

import android.util.Log

class Logger {

  def d(log: String)(implicit logTag: String = null, DisableLogging: Boolean = false) = {
    if (!DisableLogging)
      Log.d(logTag, log)
  }

  def i(log: String)(implicit logTag: String = null, DisableLogging: Boolean = false) = {
    if (!DisableLogging)
      Log.i(logTag, log)
  }

  def v(log: String)(implicit logTag: String = null, DisableLogging: Boolean = false) = {
    if (!DisableLogging)
      Log.v(logTag, log)
  }

  def e(log: String)(implicit logTag: String = null, DisableLogging: Boolean = false) = {
    if (!DisableLogging)
      Log.e(logTag, log)
  }

  def wtf(log: String)(implicit logTag: String = null, DisableLogging: Boolean = false) = {
    if (!DisableLogging)
      Log.wtf(logTag, log)
  }

}

object Logger extends Logger {

}