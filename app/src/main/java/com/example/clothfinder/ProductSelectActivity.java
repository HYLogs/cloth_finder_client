package com.example.clothfinder;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class ProductSelectActivity extends AppCompatActivity {
    private GridView gridView = null;
    private GridViewAdapter adapter = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_page);

        Toolbar tb = (Toolbar) findViewById(R.id.tb_product);
        setSupportActionBar(tb);

        Uri targetImageUri = getIntent().getParcelableExtra("targetImageUri");
        ImageView targetImageView = (ImageView) findViewById(R.id.targetImage);

        Glide.with(getApplicationContext()).load(targetImageUri).override(500).into(targetImageView);

        gridView = (GridView) findViewById((R.id.gridView));
        adapter = new GridViewAdapter();

        ArrayList<Product> products = (ArrayList<Product>) getIntent().getSerializableExtra("products");

        for (int i = 0; i < products.size(); i++) {
            adapter.addItem(products.get(i));
        }

        gridView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.tb_main, menu);

        return true;
    }

    class GridViewAdapter extends BaseAdapter {
        ArrayList<Product> items = new ArrayList<Product>();

        @Override
        public int getCount() {
            return items.size();
        }

        public void addItem(Product item) {
            items.add(item);
        }

        @Override
        public Product getItem(int pos) {
            return items.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public View getView(int pos, View convertView, ViewGroup viewGroup) {
            final Context context = viewGroup.getContext();
            final Product product = items.get(pos);
            Typeface typeface = getResources().getFont(R.font.dongle_regular);

            if(convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.gridview_list_item, viewGroup, false);
            }

            ImageView imageView = (ImageView) convertView.findViewById(R.id.item_image);
            TextView nameView = (TextView) convertView.findViewById(R.id.item_name);
            TextView priceView = (TextView) convertView.findViewById(R.id.item_price);
            TextView accuView = (TextView) convertView.findViewById(R.id.item_accuracy);

            nameView.setTypeface(typeface);
            priceView.setTypeface(typeface);
            accuView.setTypeface(typeface);

            Glide.with(convertView).load(product.getImageUri()).override(500).into(imageView);
            nameView.setText(product.getName());
            priceView.setText(Integer.toString(product.getPrice()) + "원");
            accuView.setText(Integer.toString(product.getAccuracy()) + "% 비슷하네요!");

            //각 아이템 선택 event
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, product.getLinkUri());
                    startActivity(intent);
                }
            });

            return convertView;
        }
    }
}
