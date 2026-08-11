import * as ImagePicker from 'expo-image-picker';
import * as FileSystem from 'expo-file-system/legacy';
import * as Crypto from 'expo-crypto';
import { AuthService } from './authService';

export interface LocalPickedImage {
  id: string;
  localUri: string;
  originalFilename: string;
  mimeType: string;
  byteSize: number;
  width?: number;
  height?: number;
}

export class ImageService {
  private static getImagesDirectory(): string {
    return `${FileSystem.documentDirectory}images/`;
  }

  private static async ensureDirectoryExists(): Promise<void> {
    const dir = this.getImagesDirectory();
    const info = await FileSystem.getInfoAsync(dir);
    if (!info.exists) {
      await FileSystem.makeDirectoryAsync(dir, { intermediates: true });
    }
  }

  static async pickImage(source: 'camera' | 'library'): Promise<LocalPickedImage | null> {
    await this.ensureDirectoryExists();

    let result: ImagePicker.ImagePickerResult;

    if (source === 'camera') {
      const permission = await ImagePicker.requestCameraPermissionsAsync();
      if (!permission.granted) {
        throw new Error('Camera permission required');
      }
      result = await ImagePicker.launchCameraAsync({
        mediaTypes: ImagePicker.MediaTypeOptions.Images,
        quality: 0.8,
        allowsEditing: false,
      });
    } else {
      const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
      if (!permission.granted) {
        throw new Error('Media library permission required');
      }
      result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ImagePicker.MediaTypeOptions.Images,
        quality: 0.8,
        allowsEditing: false,
      });
    }

    if (result.canceled || !result.assets || result.assets.length === 0) {
      return null;
    }

    const asset = result.assets[0];
    const imageId = Crypto.randomUUID();
    const ext = asset.uri.split('.').pop() || 'jpg';
    const destPath = `${this.getImagesDirectory()}${imageId}.${ext}`;

    await FileSystem.copyAsync({
      from: asset.uri,
      to: destPath,
    });

    const fileInfo = await FileSystem.getInfoAsync(destPath);
    const mimeType = asset.mimeType || `image/${ext === 'png' ? 'png' : 'jpeg'}`;
    const filename = asset.fileName || `${imageId}.${ext}`;

    return {
      id: imageId,
      localUri: destPath,
      originalFilename: filename,
      mimeType,
      byteSize: fileInfo.exists && 'size' in fileInfo ? fileInfo.size : 100000,
      width: asset.width,
      height: asset.height,
    };
  }

  static async uploadNoteImage(noteId: string, imageId: string, localUri: string): Promise<any> {
    const baseUrl = await AuthService.getApiUrl();
    const token = await AuthService.getToken();

    const uploadUrl = `${baseUrl}/v1/notes/${noteId}/images`;

    const response = await FileSystem.uploadAsync(uploadUrl, localUri, {
      httpMethod: 'POST',
      uploadType: FileSystem.FileSystemUploadType.MULTIPART,
      fieldName: 'file',
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'application/json',
      },
      parameters: {
        image_id: imageId,
      },
    });

    if (response.status < 200 || response.status >= 300) {
      throw new Error(`Upload failed with status ${response.status}: ${response.body}`);
    }

    return JSON.parse(response.body);
  }

  static async uploadTransactionReceipt(transactionId: string, localUri: string): Promise<any> {
    const baseUrl = await AuthService.getApiUrl();
    const token = await AuthService.getToken();

    const uploadUrl = `${baseUrl}/v1/transactions/${transactionId}/receipt`;

    const response = await FileSystem.uploadAsync(uploadUrl, localUri, {
      httpMethod: 'POST',
      uploadType: FileSystem.FileSystemUploadType.MULTIPART,
      fieldName: 'file',
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'application/json',
      },
    });

    if (response.status < 200 || response.status >= 300) {
      throw new Error(`Receipt upload failed with status ${response.status}: ${response.body}`);
    }

    return JSON.parse(response.body);
  }
}
