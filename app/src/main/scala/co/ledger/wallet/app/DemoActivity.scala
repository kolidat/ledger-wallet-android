/**
 *
 * DemoActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 19/11/15.
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
package co.ledger.wallet.app

import android.content.DialogInterface.OnClickListener
import android.content.{DialogInterface, Intent}
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.{Fragment, FragmentPagerAdapter}
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.{LinearLayoutManager, RecyclerView}
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{ProgressBar, TextView}
import co.ledger.wallet.R
import co.ledger.wallet.common._
import co.ledger.wallet.core.base.{BaseActivity, BaseFragment, WalletActivity}
import co.ledger.wallet.core.event.MainThreadEventReceiver
import co.ledger.wallet.core.utils.TR
import co.ledger.wallet.core.utils.logs.{Loggable, Logger}
import co.ledger.wallet.core.widget.DividerItemDecoration
import co.ledger.wallet.wallet.events.PeerGroupEvents._
import co.ledger.wallet.wallet.events.WalletEvents._
import co.ledger.wallet.wallet.exceptions._
import co.ledger.wallet.wallet.{Account, DerivationPath, ExtendedPublicKeyProvider, Operation}
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.params.MainNetParams

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Future, Promise}

class DemoActivity extends BaseActivity with WalletActivity {

  val XPubs = Array(
    "xpub6D4waFVPfPCpRvPkQd9A6n65z3hTp6TvkjnBHG5j2MCKytMuadKgfTUHqwRH77GQqCKTTsUXSZzGYxMGpWpJBdYAYVH75x7yMnwJvra1BUJ",
    "xpub6D4waFVPfPCpUjYZexFNXjxusXSa5WrRj2iU8v5U6x2EvVuHaSKuo1zQEJA6Lt9dRcjgM1CSQmyq3tmSj5jCSup6WC24vRrHrBUyZkv5Jem",
    "xpub6D4waFVPfPCpX183njE1zjMayNCAnMHV4D989WsFd8ENDwfcdogPfRXSaA4opz3qoLoyCZCHZy9F7GQQnBxF4nNmZfXKKiokb2ABY8Bi8Jz"
  )

  lazy val viewPager = TR(R.id.viewpager).as[ViewPager]
  lazy val tabLayout = TR(R.id.tabs).as[TabLayout]
  private lazy val viewPagerAdapter = new ViewPagerAdapter

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.demo_activity)
    viewPagerAdapter.addFragment(DemoOverviewFragment())
    viewPager.setAdapter(viewPagerAdapter)
    viewPager.setOffscreenPageLimit(100) // Whew!
    tabLayout.setupWithViewPager(viewPager)
  }

  override def onResume(): Unit = {
    super.onResume()
    wallet.synchronize(_xpubProvider).recover {
      case AccountHasNoXpubException(index) =>
        showMissingXpubDialog(index)
      case exception =>
        exception.printStackTrace()
    }
    updateAccounts()
  }

  private class ViewPagerAdapter extends FragmentPagerAdapter(getSupportFragmentManager) {
    var fragments = Array[Fragment with TabTitleHolder]()

    override def getItem(position: Int): Fragment = fragments(position)

    override def getCount: Int = fragments.length

    def addFragment(fragment: Fragment with TabTitleHolder): Unit = {
      fragments = fragments :+ fragment
    }

    def accountFragmentCount = fragments.count(_.isInstanceOf[DemoAccountFragment])

    def accountFragment(accountIndex: Int): Option[DemoAccountFragment] = {
      fragments foreach {
        case accountFragment: DemoAccountFragment =>
          if (accountFragment.accountIndex == accountIndex)
            return Some(accountFragment)
        case fragment =>
      }
      None
    }

    def removeAccountFragments(count: Int): Unit = {
      val fragmentCount = accountFragmentCount
      for (i <- 0 until count) {
        val f = accountFragment(fragmentCount - i - 1)
        if (f.isDefined)
          fragments = fragments.filter(_ != f.get)
      }
    }

    override def getPageTitle(position: Int): CharSequence = fragments(position).tabTitle
  }

  private[this] def updateAccounts(): Unit = {
    wallet.accounts().map({(accounts) =>
      _accounts = Some(accounts)
      val count = accounts.length
      if (count > viewPagerAdapter.accountFragmentCount) {
        for (i <- viewPagerAdapter.accountFragmentCount until count) {
          viewPagerAdapter.addFragment(DemoAccountFragment(i))
        }
      } else if (count < viewPagerAdapter.accountFragmentCount) {
        viewPagerAdapter.removeAccountFragments(viewPagerAdapter.accountFragmentCount - count)
      }
      viewPagerAdapter.notifyDataSetChanged()
      tabLayout.setupWithViewPager(viewPager)
    }) recover {
      case throwable: Throwable => throwable.printStackTrace()
    }

  }


  private[this] def showMissingXpubDialog(index: Int): Unit = {

  }


  override def receive: Receive = {
    case AccountCreated(index) => updateAccounts()
    case AccountUpdated(index) =>
    case string: String =>
    case drop =>
  }

  private val _xpubProvider = new ExtendedPublicKeyProvider {
    override def generateXpub(path: DerivationPath): Future[DeterministicKey] = Future.successful() flatMap {(_) =>
      val index = path(2).get.childNum
      val promise = Promise[DeterministicKey]()
      new AlertDialog.Builder(DemoActivity.this)
        .setMessage(s"Account #$index has no xpub yet\nWould you like to provide one?")
        .setPositiveButton("yes", new OnClickListener {
          override def onClick(dialog: DialogInterface, which: Int): Unit = {
            promise.success(DeterministicKey.deserializeB58(XPubs(index.toInt), MainNetParams.get()))
          }
        }).setNegativeButton("no", new OnClickListener {
        override def onClick(dialog: DialogInterface, which: Int): Unit =
          promise.failure(new Exception("I said no!"))
      }).show()
      promise.future
    }
  }

  def account(index: Int): Option[Account] = _accounts.getOrElse(Array()).lift(index)
  private[this] var _accounts: Option[Array[Account]] = None

}

class DemoHomeFragment extends BaseFragment {
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.demo_home_tab, container, false)
  }
}

class DemoAccountFragment extends BaseFragment with TabTitleHolder {

  val IndexArgsKey = "IndexArgsKey"

  lazy val accountIndex = getArguments.getInt(IndexArgsKey)
  private lazy val transactionRecyclerViewAdapter = new TransactionRecyclerViewAdapter
  def accountIndexTextView = getView.findViewById(R.id.account_index).asInstanceOf[TextView]
  def balanceTextView = getView.findViewById(R.id.balance).asInstanceOf[TextView]
  def transactionRecyclerView = getView.findViewById(R.id.transactions).asInstanceOf[RecyclerView]

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.demo_account_tab, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)
    transactionRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity))
    transactionRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity, null))
  }

  override def onResume(): Unit = {
    super.onResume()
    accountIndexTextView.setText(s"Account #${account.index}")
    account balance() map {(balance) =>
      balanceTextView.setText(s"Balance: ${balance.toFriendlyString}")
    }
    transactionRecyclerView.setAdapter(transactionRecyclerViewAdapter)
    account operations() map {(cursor) =>
      //Logger.d(s"Received ops ${operations.length}")
      //transactionRecyclerViewAdapter.clear()
      //transactionRecyclerViewAdapter.append(operations)
    }
  }


  override def onPause(): Unit = {
    super.onPause()
    //transactionRecyclerView.setAdapter(null)
  }

  private class TransactionRecyclerViewAdapter extends RecyclerView.Adapter[TransactionViewHolder] {

    private val _transactions = new ArrayBuffer[Operation]()
    private lazy val _inflater = LayoutInflater.from(getActivity)

    override def getItemCount: Int = _transactions.length

    def append(tx: Operation): Unit = {
      _transactions += tx
      notifyDataSetChanged()
    }

    def append(txs: Seq[Operation]): Unit = {
      _transactions ++= txs
      notifyDataSetChanged()
      Logger.d(s"Append ${txs.length}")
    }

    def clear(): Unit = {
      _transactions.clear()
    }

    override def onBindViewHolder(holder: TransactionViewHolder, position: Int): Unit = {
      holder.refresh(_transactions(position))
    }

    override def onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder = {
      val v = _inflater.inflate(R.layout.demo_transaction_card, parent, false)
      new TransactionViewHolder(v)
    }
  }

  private class TransactionViewHolder(v: View) extends RecyclerView.ViewHolder(v) {

    lazy val address = v.findViewById(R.id.address).asInstanceOf[TextView]
    lazy val amount = v.findViewById(R.id.amount).asInstanceOf[TextView]

    def refresh(operation: Operation): Unit = {
      if (operation.isSending) {
        address.setText(operation.recipients.lift(0).getOrElse("Unknown").toString)
        amount.setText(s"-${operation.amount.toPlainString}")
      } else {
        address.setText(operation.senders.lift(0).getOrElse("Unknown").toString)
        amount.setText(s"+${operation.amount.toPlainString}")
      }
      v.setOnClickListener({(v: View) =>
        val intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s"https://blockchain" +
          s".info/tx/${operation.hash.toString}"))
        getActivity.startActivity(intent)
      })
    }

  }

  lazy val account = getActivity.asInstanceOf[DemoActivity].account(accountIndex).get // Bad bad bad
  override def tabTitle: String = s"Account #$accountIndex"
}

trait TabTitleHolder {
  def tabTitle: String
}

object DemoAccountFragment {

  def apply(accountIndex: Int): DemoAccountFragment = {
    val fragment = new DemoAccountFragment
    val arguments = new Bundle()
    arguments.putInt(fragment.IndexArgsKey, accountIndex)
    fragment.setArguments(arguments)
    fragment
  }

}

class DemoOverviewFragment extends BaseFragment with TabTitleHolder with MainThreadEventReceiver with Loggable {

  lazy val lastBlockTimeTextView = TR(R.id.last_block_date).as[TextView]
  lazy val progress = TR(R.id.progress).as[ProgressBar]
  //lazy val accountCount = TR(R.id.account_count).as[TextView]
  //lazy val accountsList = TR(R.id.accounts).as[TextView]
  lazy val balanceTextView = TR(R.id.balance).as[TextView]

  override def tabTitle: String = "Overview"

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.demo_home_tab, container, false)
  }


  override def onResume(): Unit = {
    super.onResume()
    register(wallet.eventBus)
    updateBalance()
  }

  override def onPause(): Unit = {
    super.onPause()
    unregister(wallet.eventBus)
  }

  private[this] def updateBalance(): Unit = {
    wallet.balance().map {(b) =>
      balanceTextView.setText(s"Balance: ${b.toFriendlyString}")
    } recover {
      case throwable: Throwable =>
        throwable.printStackTrace()
    }
  }

  override def receive: Receive = {
    case CoinReceived(index, _) => updateBalance()
    case CoinSent(index, _) => updateBalance()
    case AccountUpdated(index) => updateBalance()
    case SynchronizationProgress(current, total) =>
      progress.setMax(total)
      progress.setProgress(current)
    case BlockDownloaded(block) =>
      lastBlockTimeTextView.setText(s"Last block time: ${block.getTime.toString}")
    case event =>
  }

  def wallet = getActivity.asInstanceOf[DemoActivity].wallet
}

object DemoOverviewFragment {

  def apply() = new DemoOverviewFragment

}