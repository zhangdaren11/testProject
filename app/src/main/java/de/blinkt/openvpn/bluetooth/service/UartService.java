/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.blinkt.openvpn.bluetooth.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import de.blinkt.openvpn.core.ICSOpenVPNApplication;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class UartService extends Service implements Serializable {
	private final static String TAG = UartService.class.getSimpleName();

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;
	public int mConnectionState = STATE_DISCONNECTED;

	private static final int STATE_DISCONNECTED = 0;
	private static final int STATE_CONNECTING = 1;
	public static final int STATE_CONNECTED = 2;

	public final static String ACTION_GATT_CONNECTED =
			"com.nordicsemi.nrfUART.ACTION_GATT_CONNECTED";//蓝牙连接
	public final static String ACTION_GATT_DISCONNECTED =
			"com.nordicsemi.nrfUART.ACTION_GATT_DISCONNECTED";//蓝牙断开
	public final static String ACTION_GATT_SERVICES_DISCOVERED =
			"com.nordicsemi.nrfUART.ACTION_GATT_SERVICES_DISCOVERED";//没有找到蓝牙设备
	public final static String ACTION_DATA_AVAILABLE =
			"com.nordicsemi.nrfUART.ACTION_DATA_AVAILABLE";//蓝牙往手机发送数据
	public final static String EXTRA_DATA =
			"com.nordicsemi.nrfUART.EXTRA_DATA";//蓝牙发送额外数据
	public final static String DEVICE_DOES_NOT_SUPPORT_UART =
			"com.nordicsemi.nrfUART.DEVICE_DOES_NOT_SUPPORT_UART";//不支持操作

	public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	public static final UUID RX_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
	public static final UUID RX_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
	public static final UUID TX_CHAR_UUID1 = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
//	public static final UUID TX_CHAR_UUID2 = UUID.fromString("6E400004-B5A3-F393-E0A9-E50E24DCCA9F");
//	public static final UUID TX_CHAR_UUID3 = UUID.fromString("6E400005-B5A3-F393-E0A9-E50E24DCCA9F");
	private List<BluetoothGattService> BluetoothGattServices;
	// Implements callback methods for GATT events that the app cares about.  For example,
	// Implements callback methods for GATT events that the app cares about.  For example,
	// connection change and services discovered.
	//蓝牙回调
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		//监听蓝牙连接状态
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			String intentAction;

			if (newState == BluetoothProfile.STATE_CONNECTED) {
				intentAction = ACTION_GATT_CONNECTED;
				mConnectionState = STATE_CONNECTED;
				broadcastUpdate(intentAction);
				Log.i(TAG, "Connected to GATT server.");
				// Attempts to discover services after successful connection.
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				boolean isFindServiceSuccess = mBluetoothGatt.discoverServices();
				Log.i(TAG, "Attempting to start service discovery:" + isFindServiceSuccess);

			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				intentAction = ACTION_GATT_DISCONNECTED;
				mConnectionState = STATE_DISCONNECTED;
				Log.i(TAG, "Disconnected from GATT server.");
				broadcastUpdate(intentAction);
			}
		}

		//监听搜索蓝牙设备
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.w(TAG, "mBluetoothGatt = " + mBluetoothGatt);
				BluetoothGattServices = mBluetoothGatt.getServices();
				Log.e("getService", "mBluetoothGatt size = " + BluetoothGattServices.size());
				broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
			} else {
				Log.w(TAG, "onServicesDiscovered received: " + status);
			}
		}

		//蓝牙发送数据到应用
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
										 BluetoothGattCharacteristic characteristic,
										 int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
											BluetoothGattCharacteristic characteristic) {
			broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
		}
	};
	private BluetoothGattService RxService;
	private BluetoothGattDescriptor descriptor;

	//发送广播
	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent(action);
		LocalBroadcastManager.getInstance(ICSOpenVPNApplication.getContext()).sendBroadcast(intent);
	}

	//发送广播
	private void broadcastUpdate(final String action,
								 final BluetoothGattCharacteristic characteristic) {
		final Intent intent = new Intent(action);

		// This is special handling for the Heart Rate Measurement profile.  Data parsing is
		// carried out as per profile specifications:
		// http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
		if (TX_CHAR_UUID1.equals(characteristic.getUuid())
//				|| TX_CHAR_UUID2.equals(characteristic.getUuid())
//				|| TX_CHAR_UUID3.equals(characteristic.getUuid())
				) {
			// Log.d(TAG, String.format("Received TX: %d",characteristic.getValue() ));
			intent.putExtra(EXTRA_DATA, characteristic.getValue());
		}
		LocalBroadcastManager.getInstance(ICSOpenVPNApplication.getContext()).sendBroadcast(intent);
	}


	public class LocalBinder extends Binder {
		public UartService getService() {
			return UartService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// After using a given device, you should make sure that BluetoothGatt.close() is called
		// such that resources are cleaned up properly.  In this particular example, close() is
		// invoked when the UI is disconnected from the Service.
		close();
		return super.onUnbind(intent);
	}

	private final IBinder mBinder = new LocalBinder();

	/**
	 * Initializes a reference to the local Bluetooth adapter.
	 *
	 * @return Return true if the initialization is successful.
	 */
	//初始化蓝牙
	public boolean initialize() {
		// For API level 18 and above, get a reference to BluetoothAdapter through
		// BluetoothManager.
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				Log.e(TAG, "Unable to initialize BluetoothManager.");
				return false;
			}
		}

		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
			return false;
		}

		return true;
	}

	/**
	 * Connects to the GATT server hosted on the Bluetooth LE device.
	 *
	 * @param address The device address of the destination device.
	 * @return Return true if the connection is initiated successfully. The connection result
	 * is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	//通过mac地址连接设备
	public boolean connect(final String address) {
		if (mBluetoothAdapter == null || address == null) {
			Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
			return false;
		}


//		// Previously connected device.  Try to reconnect.
//		if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
//				&& mBluetoothGatt != null) {
//			Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
//			if (mBluetoothGatt.connect()) {
//				mConnectionState = STATE_CONNECTING;
//				return true;
//			} else {
//				return false;
//			}
//		}

		final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (device == null) {
			Log.w(TAG, "Device not found.  Unable to connect.");
			return false;
		}
		if (mBluetoothGatt != null) {
			mBluetoothGatt.close();
			//LogUtil.info("-------------关闭mBluetoothGatt");
		}
		// We want to directly connect to the device, so we are setting the autoConnect
		// parameter to false.
		mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
		Log.d(TAG, "Trying to create a new connection.");
		mBluetoothDeviceAddress = address;
		mConnectionState = STATE_CONNECTING;
		return true;
	}

	/**
	 * Disconnects an existing connection or cancel a pending connection. The disconnection result
	 * is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	//断开连接
	public void disconnect() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.disconnect();
		mConnectionState = STATE_DISCONNECTED;
		close();
//		mBluetoothGatt.close();
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure resources are
	 * released properly.
	 */

	public void close() {
		if (mBluetoothGatt == null) {
			return;
		}
		Log.w(TAG, "mBluetoothGatt closed");
		mBluetoothDeviceAddress = null;
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}

	/**
	 * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
	 * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 *
	 * @param characteristic The characteristic to read from.
	 */
	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.readCharacteristic(characteristic);
	}

	/**
	 * Enables or disables notification on a give characteristic.
	 *
	 * @param characteristic Characteristic to act on.
	 * @param enabled If true, enable notification.  False otherwise.
	 */
	/*
	public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);


        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }*/

	/**
	 * Enable TXNotification
	 *
	 * @return
	 */
	public void enableTXNotification() {
		if (mBluetoothGatt == null) {
			showMessage("mBluetoothGatt null" + mBluetoothGatt);
			broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
			return;
		}
		BluetoothGattService RxService = null;
		RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
		Log.i("getService", "获取服务："+RxService);
		if (RxService == null) {
			int j = 0;
			for (int i = 0; i < BluetoothGattServices.size(); i++) {
				Log.i("getService", "获取服务群"+ ++ j);
				RxService = BluetoothGattServices.get(i);
				if (RxService != null&&RxService.getCharacteristic(TX_CHAR_UUID1)!=null) {
					if(i==BluetoothGattServices.size()-1)
					{
						RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
					}
					break;
				}
			}

		}
		if (RxService == null) {
			showMessage("Rx service not found!");
			broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
			return;
		}
		setDescriptor(RxService, TX_CHAR_UUID1);
//		setDescriptor(RxService, TX_CHAR_UUID2);
//		setDescriptor(RxService, TX_CHAR_UUID3);
	}

	//用于某个接收的UUID写入mBluetoothGatt的监听callback里面。在onCharacteristicChanged()会产生响应
	public void setDescriptor(BluetoothGattService rxService, UUID uuid) {
		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		BluetoothGattCharacteristic TxChar = rxService.getCharacteristic(uuid);
		if (TxChar == null) {
			showMessage("Tx charateristic not found!");
			broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
			return;
		}
		mBluetoothGatt.setCharacteristicNotification(TxChar, true);

		descriptor = TxChar.getDescriptor(CCCD);
		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//		mBluetoothGatt.readDescriptor(descriptor);
		mBluetoothGatt.writeDescriptor(descriptor);
	}

	public void writeRXCharacteristic(byte[] value) {
		//如果mBluetoothGatt为空，意味着连接中断，所以不允许继续传输数据
		if (mBluetoothGatt == null) {
			Log.e("Blue_Chanl", "蓝牙已断开，发送失败！");
			return;
		}
		if (RxService == null) {
			RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
		}
		showMessage("mBluetoothGatt null" + mBluetoothGatt);
		if (RxService == null) {
			showMessage("Rx service not found!");
			broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
			return;
		}
		BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
		if (RxChar == null) {
			showMessage("Rx charateristic not found!");
			broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
			return;
		}
		RxChar.setValue(value);
		boolean status = mBluetoothGatt.writeCharacteristic(RxChar);

		Log.e("Blue_Chanl", "write TXchar - status=" + status);
		if (!status) {
			try {
				Thread.sleep(500);
				status = mBluetoothGatt.writeCharacteristic(RxChar);
				Log.e("Blue_Chanl", "重发：write TXchar - status=" + status);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void showMessage(String msg) {
		Log.e(TAG, msg);
	}

	/**
	 * Retrieves a list of supported GATT services on the connected device. This should be
	 * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
	 *
	 * @return A {@code List} of supported services.
	 */
	public List<BluetoothGattService> getSupportedGattServices() {
		if (mBluetoothGatt == null) return null;

		return mBluetoothGatt.getServices();
	}

	public boolean isOpenBlueTooth() {
		if (mBluetoothAdapter != null) {
			if (mBluetoothAdapter.isEnabled()) {
				return true;
			}
		}
		return false;
	}

	public boolean isConnecttingBlueTooth() {
		return mConnectionState == STATE_CONNECTING;
	}

}
