/**
 *
 * WalletService
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 24/11/15.
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
package co.ledger.wallet.service.wallet

import android.app.Service
import android.content.Intent
import android.os.IBinder
import co.ledger.wallet.service.wallet.spv.SpvWalletClient
import co.ledger.wallet.wallet.Wallet
import org.bitcoinj.params.MainNetParams

class WalletService extends Service {

  private var _wallet: SpvWalletClient = null // Temporary implementation
  def wallet(name: String): Wallet = {
    if (_wallet == null)
      _wallet = new SpvWalletClient(this, name, MainNetParams.get())
    _wallet
  }

  override def onCreate(): Unit = {
    super.onCreate()
    this.startService(new Intent(this, this.getClass))
  }

  override def onBind(intent: Intent): IBinder = _binder

  class Binder extends android.os.Binder {
    def service = WalletService.this
  }

  private[this] val _binder = new Binder

}

