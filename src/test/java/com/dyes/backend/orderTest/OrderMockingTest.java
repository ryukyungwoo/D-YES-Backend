package com.dyes.backend.orderTest;

import com.dyes.backend.domain.authentication.service.AuthenticationService;
import com.dyes.backend.domain.cart.entity.Cart;
import com.dyes.backend.domain.cart.entity.ContainProductOption;
import com.dyes.backend.domain.cart.repository.CartRepository;
import com.dyes.backend.domain.cart.repository.ContainProductOptionRepository;
import com.dyes.backend.domain.cart.service.CartService;
import com.dyes.backend.domain.farm.entity.Farm;
import com.dyes.backend.domain.order.controller.form.OrderConfirmRequestForm;
import com.dyes.backend.domain.order.controller.form.OrderConfirmResponseForm;
import com.dyes.backend.domain.order.controller.form.OrderProductInCartRequestForm;
import com.dyes.backend.domain.order.controller.form.OrderProductInProductPageRequestForm;
import com.dyes.backend.domain.order.entity.OrderedProduct;
import com.dyes.backend.domain.order.repository.OrderRepository;
import com.dyes.backend.domain.order.repository.OrderedProductRepository;
import com.dyes.backend.domain.order.repository.OrderedPurchaserProfileRepository;
import com.dyes.backend.domain.order.service.OrderServiceImpl;
import com.dyes.backend.domain.order.service.request.OrderConfirmRequest;
import com.dyes.backend.domain.order.service.request.OrderedProductOptionRequest;
import com.dyes.backend.domain.order.service.request.OrderedPurchaserProfileRequest;
import com.dyes.backend.domain.order.service.response.OrderConfirmProductResponse;
import com.dyes.backend.domain.order.service.response.OrderConfirmUserResponse;
import com.dyes.backend.domain.product.entity.*;
import com.dyes.backend.domain.product.repository.ProductMainImageRepository;
import com.dyes.backend.domain.product.repository.ProductOptionRepository;
import com.dyes.backend.domain.user.entity.*;
import com.dyes.backend.domain.user.repository.UserProfileRepository;
import com.dyes.backend.domain.user.repository.UserRepository;
import com.dyes.backend.utility.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest

public class OrderMockingTest {
    @Mock
    private OrderRepository mockOrderRepository;
    @Mock
    private ContainProductOptionRepository mockContainProductOptionRepository;
    @Mock
    private CartService mockCartService;
    @Mock
    private CartRepository mockCartRepository;
    @Mock
    private ProductOptionRepository mockProductOptionRepository;
    @Mock
    private AuthenticationService mockAuthenticationService;
    @Mock
    private UserProfileRepository mockUserProfileRepository;
    @Mock
    private ProductMainImageRepository mockProductMainImageRepository;
    @Mock
    private OrderedProductRepository mockOrderedProductRepository;
    @Mock
    private RedisService mockRedisService;
    @Mock
    private UserRepository mockUserRepository;
    @Mock
    OrderedPurchaserProfileRepository mockOrderedPurchaserProfileRepository;
    @InjectMocks
    private OrderServiceImpl mockOrderService;
    @BeforeEach
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockOrderService = new OrderServiceImpl(
                mockOrderRepository,
                mockCartRepository,
                mockProductOptionRepository,
                mockContainProductOptionRepository,
                mockUserProfileRepository,
                mockProductMainImageRepository,
                mockOrderedProductRepository,
                mockOrderedPurchaserProfileRepository,
                mockCartService,
                mockAuthenticationService
        );
    }
    @Test
    @DisplayName("order mocking test: order product in cart")
    public void 사용자가_장바구니에서_물품을_주문합니다 () {
        final String userToken = "google 유저";
        final String accessToken = "엑세스 토큰";
        final int totalAmount = 1;
        OrderProductInCartRequestForm requestForm = new OrderProductInCartRequestForm(
                userToken,
                new OrderedPurchaserProfileRequest("구매자 이름", "전화 번호", "이메일", "주소", "코드", "주소 디테일"),
                List.of(new OrderedProductOptionRequest(1L, 1)), 1
        );
        OrderedProduct orderedProduct = new OrderedProduct();
        orderedProduct.setProductOptionId(requestForm.getOrderedProductOptionRequestList().get(0).getProductOptionId());
        requestForm.setUserToken(userToken);
        when(mockRedisService.getAccessToken(userToken)).thenReturn(accessToken);
        User user = new User();
        when(mockUserRepository.findByAccessToken(accessToken)).thenReturn(Optional.of(user));

        when(mockAuthenticationService.findUserByUserToken(userToken)).thenReturn(user);
        Cart cart = new Cart(1L, user);
        when(mockCartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(mockCartService.cartCheckFromUserToken(userToken)).thenReturn(cart);
        ContainProductOption containProductOption = new ContainProductOption();
        containProductOption.setId(1L);
        containProductOption.setCart(cart);
        when(mockContainProductOptionRepository.findAllByCart(cart)).thenReturn(List.of(containProductOption));

        boolean actual = mockOrderService.orderProductInCart(requestForm);
        assertTrue(actual);
    }
    @Test
    @DisplayName("order mocking test: order product in product page")
    public void 사용자가_상품_페이지에서_물품을_주문합니다 () {
        final String userToken = "google 유저";
        final String accessToken = "엑세스 토큰";
        final int totalAmount = 1;
        OrderProductInProductPageRequestForm requestForm = new OrderProductInProductPageRequestForm(
                userToken,
                new OrderedPurchaserProfileRequest("구매자 이름", "전화 번호", "이메일", "주소", "코드", "주소 디테일"),
                List.of(new OrderedProductOptionRequest(1L, 1)), 1
        );

        requestForm.setUserToken(userToken);
        when(mockRedisService.getAccessToken(userToken)).thenReturn(accessToken);
        User user = new User();
        when(mockUserRepository.findByAccessToken(accessToken)).thenReturn(Optional.of(user));

        when(mockAuthenticationService.findUserByUserToken(userToken)).thenReturn(user);
        Cart cart = new Cart();
        when(mockCartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        boolean actual = mockOrderService.orderProductInProductPage(requestForm);
        assertTrue(actual);
    }
    @Test
    @DisplayName("order mocking test: confirm order")
    public void 사용자가_주문하기_전에_주문_확인을_할_수_있습니다 () {
        final String userToken = "google 유저";

        OrderConfirmRequestForm requestForm = new OrderConfirmRequestForm(userToken);
        OrderConfirmRequest request = new OrderConfirmRequest(requestForm.getUserToken());

        User user = new User("1", "엑세스토큰", "리프래시 토큰", Active.YES, UserType.GOOGLE);
        when(mockAuthenticationService.findUserByUserToken(request.getUserToken())).thenReturn(user);
        UserProfile userProfile = new UserProfile("아이디", "닉네임", "이메일", "프로필 사진", "전화번호", new Address(), user);
        when(mockUserProfileRepository.findByUser(user)).thenReturn(Optional.of(userProfile));

        Cart cart = new Cart(1L, user);
        when(mockCartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        Product product = new Product(1L, "상품 이름", "상세 설명", CultivationMethod.ORGANIC, SaleStatus.AVAILABLE, new Farm());
        ProductOption productOption = new ProductOption(1L, "옵션이름", 1L, 1, new Amount(), product, SaleStatus.AVAILABLE);
        ContainProductOption containProductOption = new ContainProductOption(1L, cart, "상품 이름", 1L, "상품 이미지", 1L, "옵션 이름", 1L, 1);
        when(mockContainProductOptionRepository.findAllByCart(cart)).thenReturn(List.of(containProductOption));
        when(mockProductOptionRepository.findByIdWithProduct(containProductOption.getId())).thenReturn(Optional.of(productOption));
        ProductMainImage mainImage = new ProductMainImage(1L, "메인 이미지", product);
        when(mockProductMainImageRepository.findByProductId(product.getId())).thenReturn(Optional.of(mainImage));

        OrderConfirmUserResponse userResponse = OrderConfirmUserResponse.builder()
                .email(userProfile.getEmail())
                .contactNumber(userProfile.getContactNumber())
                .address(userProfile.getAddress().getAddress())
                .zipCode(userProfile.getAddress().getZipCode())
                .addressDetail(userProfile.getAddress().getAddressDetail())
                .build();

        OrderConfirmProductResponse productResponse = OrderConfirmProductResponse.builder()
                .productName(productOption.getProduct().getProductName())
                .optionId(productOption.getId())
                .optionPrice(productOption.getOptionPrice())
                .optionCount(containProductOption.getOptionCount())
                .productMainImage(mainImage.getMainImg())
                .value(productOption.getAmount().getValue())
                .unit(productOption.getAmount().getUnit())
                .build();
        OrderConfirmResponseForm responseForm = new OrderConfirmResponseForm(userResponse, List.of(productResponse));

        OrderConfirmResponseForm actual = mockOrderService.orderConfirm(requestForm);

        assertEquals(responseForm.getUserResponse().getEmail(), actual.getUserResponse().getEmail());
    }
}
