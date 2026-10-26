declare namespace API {
  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseInteger = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponseListImageSearchResult = {
    code?: number
    data?: ImageSearchResult[]
    message?: string
  }

  type BaseResponseListPictureVO = {
    code?: number
    data?: PictureVO[]
    message?: string
  }

  type BaseResponseListSpace = {
    code?: number
    data?: Space[]
    message?: string
  }

  type BaseResponseListSpaceCategoryAnalyzeResponse = {
    code?: number
    data?: SpaceCategoryAnalyzeResponse[]
    message?: string
  }

  type BaseResponseListSpaceLevel = {
    code?: number
    data?: SpaceLevel[]
    message?: string
  }

  type BaseResponseListSpaceSizeAnalyzeResponse = {
    code?: number
    data?: SpaceSizeAnalyzeResponse[]
    message?: string
  }

  type BaseResponseListSpaceTagAnalyzeResponse = {
    code?: number
    data?: SpaceTagAnalyzeResponse[]
    message?: string
  }

  type BaseResponseListSpaceUserAnalyzeResponse = {
    code?: number
    data?: SpaceUserAnalyzeResponse[]
    message?: string
  }

  type BaseResponseListSpaceUserVO = {
    code?: number
    data?: SpaceUserVO[]
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponsePagePicture = {
    code?: number
    data?: PagePicture
    message?: string
  }

  type BaseResponsePagePictureVO = {
    code?: number
    data?: PagePictureVO
    message?: string
  }

  type BaseResponsePageSpace = {
    code?: number
    data?: PageSpace
    message?: string
  }

  type BaseResponsePageSpaceVO = {
    code?: number
    data?: PageSpaceVO
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponsePicture = {
    code?: number
    data?: Picture
    message?: string
  }

  type BaseResponsePictureTagCategory = {
    code?: number
    data?: PictureTagCategory
    message?: string
  }

  type BaseResponsePictureVO = {
    code?: number
    data?: PictureVO
    message?: string
  }

  type BaseResponseSpaceUsageAnalyzeResponse = {
    code?: number
    data?: SpaceUsageAnalyzeResponse
    message?: string
  }

  type BaseResponseSpaceUser = {
    code?: number
    data?: SpaceUser
    message?: string
  }

  type BaseResponseSpaceVO = {
    code?: number
    data?: SpaceVO
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserLoginVO = {
    code?: number
    data?: UserLoginVO
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type DeleteRequest = {
    id?: number
  }

  type EditPictureRequest = {
    id: number
    name?: string
    introduction?: string
    category?: string
    tags?: string[]
  }

  type getPictureByIdParams = {
    id: number
  }

  type getPictureVOByIdParams = {
    id: number
  }

  type getSpaceVOByIdParams = {
    id: number
  }

  type getUserByIdParams = {
    id: number
  }

  type getUserVOByIdParams = {
    id: number
  }

  type ImageSearchResult = {
    thumbUrl?: string
    fromUrl?: string
  }

  type OrderItem = {
    column?: string
    asc?: boolean
  }

  type PagePicture = {
    records?: Picture[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: PagePicture
    searchCount?: PagePicture
    optimizeJoinOfCountSql?: boolean
    maxLimit?: number
    countId?: string
    pages?: number
  }

  type PagePictureVO = {
    records?: PictureVO[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: PagePictureVO
    searchCount?: PagePictureVO
    optimizeJoinOfCountSql?: boolean
    maxLimit?: number
    countId?: string
    pages?: number
  }

  type PageSpace = {
    records?: Space[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: PageSpace
    searchCount?: PageSpace
    optimizeJoinOfCountSql?: boolean
    maxLimit?: number
    countId?: string
    pages?: number
  }

  type PageSpaceVO = {
    records?: SpaceVO[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: PageSpaceVO
    searchCount?: PageSpaceVO
    optimizeJoinOfCountSql?: boolean
    maxLimit?: number
    countId?: string
    pages?: number
  }

  type PageUserVO = {
    records?: UserVO[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: PageUserVO
    searchCount?: PageUserVO
    optimizeJoinOfCountSql?: boolean
    maxLimit?: number
    countId?: string
    pages?: number
  }

  type Picture = {
    id?: number
    url?: string
    name?: string
    introduction?: string
    category?: string
    tags?: string
    picSize?: number
    picWidth?: number
    picHeight?: number
    picScale?: number
    picFormat?: string
    userId?: number
    createTime?: string
    editTime?: string
    updateTime?: string
    reviewStatus?: number
    reviewMessage?: string
    reviewerId?: number
    reviewTime?: string
    thumbnailUrl?: string
    spaceId?: number
    originUrl?: string
    mainColor?: string
    isDelete?: number
  }

  type PictureEditBatchRequest = {
    pictureIdList?: number[]
    spaceId?: number
    category?: string
    tags?: string[]
    nameRule?: string
  }

  type PictureQueryRequest = {
    currentNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    name?: string
    introduction?: string
    category?: string
    tags?: string[]
    picSize?: number
    picWidth?: number
    picHeight?: number
    picScale?: number
    picFormat?: string
    userId?: number
    searchText?: string
    reviewStatus?: number
    reviewMessage?: string
    reviewerId?: number
    nullSpaceId?: boolean
    spaceId?: number
    startEditTime?: string
    endEditTime?: string
  }

  type PictureReviewRequest = {
    id?: number
    reviewStatus?: number
    reviewMessage?: string
  }

  type PictureTagCategory = {
    tagList?: string[]
    categoryList?: string[]
  }

  type PictureUpdateRequest = {
    id?: number
    name?: string
    introduction?: string
    category?: string
    tags?: string[]
  }

  type PictureUploadByBatchRequest = {
    searchText?: string
    number?: number
    namePrefix?: string
  }

  type PictureUploadRequest = {
    id?: number
    fileUrl?: string
    picName?: string
    spaceId?: number
  }

  type PictureVO = {
    id?: number
    url?: string
    thumbnailUrl?: string
    name?: string
    introduction?: string
    tags?: string[]
    category?: string
    picSize?: number
    picWidth?: number
    picHeight?: number
    picScale?: number
    picFormat?: string
    userId?: number
    createTime?: string
    editTime?: string
    updateTime?: string
    userVO?: UserVO
    spaceId?: number
    originUrl?: string
    mainColor?: string
    permissions?: string[]
  }

  type SearchPictureByColorRequest = {
    spaceId?: number
    mainColor?: string
  }

  type SearchPictureByPictureRequest = {
    pictureId?: number
  }

  type Space = {
    id?: number
    spaceName?: string
    spaceLevel?: number
    maxSize?: number
    maxCount?: number
    totalSize?: number
    totalCount?: number
    userId?: number
    createTime?: string
    editTime?: string
    updateTime?: string
    spaceType?: number
    isDelete?: number
  }

  type SpaceAddRequest = {
    spaceName?: string
    spaceLevel?: number
    userId?: number
    spaceType?: number
  }

  type SpaceAnalyzeRequest = {
    spaceId?: number
    queryPublic?: boolean
    queryAll?: boolean
  }

  type SpaceCategoryAnalyzeRequest = {
    spaceId?: number
    queryPublic?: boolean
    queryAll?: boolean
  }

  type SpaceCategoryAnalyzeResponse = {
    category?: string
    count?: number
    totalSize?: number
  }

  type SpaceLevel = {
    value?: number
    text?: string
    maxCount?: number
    maxSize?: number
  }

  type SpaceQueryRequest = {
    currentNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    userId?: number
    spaceName?: string
    spaceLevel?: number
    spaceType?: number
  }

  type SpaceRankAnalyzeRequest = {
    topN?: number
  }

  type SpaceSizeAnalyzeRequest = {
    spaceId?: number
    queryPublic?: boolean
    queryAll?: boolean
  }

  type SpaceSizeAnalyzeResponse = {
    sizeRange?: string
    count?: number
  }

  type SpaceTagAnalyzeRequest = {
    spaceId?: number
    queryPublic?: boolean
    queryAll?: boolean
  }

  type SpaceTagAnalyzeResponse = {
    tag?: string
    count?: number
  }

  type SpaceUpdateRequest = {
    id?: number
    spaceName?: string
    spaceLevel?: number
    maxSize?: number
    maxCount?: number
  }

  type SpaceUsageAnalyzeResponse = {
    usedSize?: number
    maxSize?: number
    sizeUsageRatio?: number
    usedCount?: number
    maxCount?: number
    countUsageRatio?: number
  }

  type SpaceUser = {
    id?: number
    spaceId?: number
    userId?: number
    spaceRole?: string
    createTime?: string
    updateTime?: string
  }

  type SpaceUserAddRequest = {
    spaceId?: number
    userId?: number
    spaceRole?: string
  }

  type SpaceUserAnalyzeRequest = {
    spaceId?: number
    queryPublic?: boolean
    queryAll?: boolean
    userId?: string
    timeDimension?: string
  }

  type SpaceUserAnalyzeResponse = {
    period?: string
    count?: number
  }

  type SpaceUserEditRequest = {
    id?: number
    spaceRole?: string
  }

  type SpaceUserQueryRequest = {
    id?: number
    spaceId?: number
    userId?: number
    spaceRole?: string
  }

  type SpaceUserVO = {
    id?: number
    spaceId?: number
    userId?: number
    spaceRole?: string
    createTime?: string
    updateTime?: string
    space?: SpaceVO
    user?: UserVO
  }

  type SpaceVO = {
    id?: number
    spaceName?: string
    spaceLevel?: number
    maxSize?: number
    maxCount?: number
    totalSize?: number
    totalCount?: number
    userId?: number
    createTime?: string
    editTime?: string
    updateTime?: string
    user?: UserVO
    permissions?: string[]
    spaceType?: number
  }

  type uploadPictureParams = {
    pictureUploadRequest: PictureUploadRequest
  }

  type User = {
    id?: number
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
    admin?: boolean
    notAdmin?: boolean
  }

  type UserAddRequest = {
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserEditRequest = {
    userName?: string
    userProfile?: string
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  type UserLoginVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
    updateTime?: string
  }

  type UserQueryRequest = {
    currentNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    userAccount?: string
    userName?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    checkPassword?: string
  }

  type UserUpdateRequest = {
    id?: number
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
  }
}
