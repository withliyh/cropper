# cropper
圆形头像剪裁库

使用方式更简单，支持图片浏览和拍照
public class MainActivity extends AppCompatActivity {

    CroperHelper croperHelper;

    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        croperHelper = new CroperHelper(this);
        croperHelper.setOnResult(new CroperHelper.OnResult() {
            @Override
            public void onSuccess(Uri uri) {
                imageView.setImageURI(uri);
            }

            @Override
            public void onFailure() {
                Toast.makeText(MainActivity.this, "失败", Toast.LENGTH_SHORT).show();
            }
        });

        imageView = (ImageView) findViewById(R.id.imageView);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                croperHelper.pickImage();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        croperHelper.onActivityResult(requestCode, resultCode, data);
    }
}
