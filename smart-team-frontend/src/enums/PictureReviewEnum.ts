/**
 * 图片审核状态枚举 审核状态：0-待审核; 1-通过; 2-拒绝
 */
export enum PictureReviewStatusEnum {
  PENDING = 0,
  PASS = 1,
  REJECT = 2
}

/**
 * 图片审核状态接口
 */
export interface PictureReviewStatusInfo {
  text: string;
  value: number;
}

/**
 * 获取所有枚举信息
 */
export const getAllPictureReviewStatus = (): PictureReviewStatusInfo[] => {
  return [
    { text: '待审核', value: PictureReviewStatusEnum.PENDING },
    { text: '通过', value: PictureReviewStatusEnum.PASS },
    { text: '拒绝', value: PictureReviewStatusEnum.REJECT }
  ];
};

/**
 * 根据value获取枚举信息
 */
export const getPictureReviewStatusByValue = (value: number | null): PictureReviewStatusInfo | null => {
  if (value === null) return null;

  const allStatus = getAllPictureReviewStatus();
  return allStatus.find(status => status.value === value) || null;
};

/**
 * 根据value获取text
 */
export const getPictureReviewStatusText = (value: number | null): string => {
  const status = getPictureReviewStatusByValue(value);
  return status?.text || '';
};

/**
 * 将图片审核状态列表转换为Options
 * - 给需要组件中Option进行使用
 * @returns 
 */
export const getPictureReviewStatusOptions = () => {
  const options = getAllPictureReviewStatus().map(status => ({ label: status.text, value: status.value }));
  return options;
}




